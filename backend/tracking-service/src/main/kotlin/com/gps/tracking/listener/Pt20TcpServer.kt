
package com.gps.tracking.protocol

import com.gps.tracking.client.DeviceJsonRpcClient
import com.gps.tracking.dto.request.LocationRequest
import com.gps.tracking.service.LocationService
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Component
class Pt20TcpServer(
    @Value("\${tracking.tcp.port:5054}")
    private val port: Int,
    private val locationService: LocationService,
    private val deviceJsonRpcClient: DeviceJsonRpcClient
) {
    private val log = LoggerFactory.getLogger(Pt20TcpServer::class.java)
    private val packetDecoder = Pt20PacketDecoder()
    private val gpsLocationDecoder = Pt20GpsLocationDecoder()

    private val running = AtomicBoolean(false)
    private val clients: ExecutorService = Executors.newCachedThreadPool()

    @Volatile
    private var serverSocket: ServerSocket? = null

    private var serverThread: Thread? = null

    @PostConstruct
    fun start() {
        if (!running.compareAndSet(false, true)) return

        serverThread = Thread({
            try {
                ServerSocket(port).use { server ->
                    serverSocket = server
                    log.info("PT20 TCP server listening on port {}", port)

                    while (running.get()) {
                        val socket = server.accept()
                        log.info(
                            "TCP client connected: {}",
                            socket.remoteSocketAddress
                        )
                        clients.submit { handleClient(socket) }
                    }
                }
            } catch (ex: SocketException) {
                if (running.get()) {
                    log.error("PT20 TCP server socket error", ex)
                }
            } catch (ex: Exception) {
                if (running.get()) {
                    log.error("PT20 TCP server failed", ex)
                }
            }
        }, "pt20-tcp-server").apply {
            isDaemon = true
            start()
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            var devicePublicId: UUID? = null

            try {
                client.soTimeout = 300_000

                val input = client.getInputStream()
                val output = client.getOutputStream()

                while (running.get() && !client.isClosed) {
                    val first = input.read()
                    if (first == -1) break

                    // Locate a supported frame header.
                    if (first != 0x78 && first != 0x79) continue

                    val second = input.read()
                    if (second == -1) break

                    if (second != first) continue

                    val isShortFrame = first == 0x78
                    val length: Int

                    if (isShortFrame) {
                        length = input.read()
                        if (length == -1) break
                    } else {
                        val high = input.read()
                        val low = input.read()

                        if (high == -1 || low == -1) break

                        length = (high shl 8) or low
                    }

                    // Length bytes plus the two stop bytes.
                    val remainingLength = length + 2
                    if (remainingLength <= 2 || remainingLength > 65537) {
                        log.warn("Invalid PT20 frame length: {}", length)
                        break
                    }

                    val packetBuffer = ByteArrayOutputStream()
                    packetBuffer.write(first)
                    packetBuffer.write(second)

                    if (isShortFrame) {
                        packetBuffer.write(length)
                    } else {
                        packetBuffer.write((length shr 8) and 0xFF)
                        packetBuffer.write(length and 0xFF)
                    }

                    val remaining = ByteArray(remainingLength)
                    var bytesRead = 0

                    while (bytesRead < remainingLength) {
                        val count = input.read(
                            remaining,
                            bytesRead,
                            remainingLength - bytesRead
                        )

                        if (count == -1) {
                            throw EOFException("Incomplete PT20 frame")
                        }

                        bytesRead += count
                    }

                    packetBuffer.write(remaining)
                    val bytes = packetBuffer.toByteArray()

                    log.info("PT20 raw packet: {}", bytes.toHex())

                    val packet = try {
                        packetDecoder.decode(bytes)
                    } catch (ex: IllegalArgumentException) {
                        log.warn("Invalid PT20 packet: {}", ex.message)
                        continue
                    }

                    log.info(
                        "PT20 decoded protocol=0x{}, serial={}, CRC=0x{}",
                        "%02X".format(packet.protocolNumber),
                        packet.serialNumber,
                        "%04X".format(packet.crc)
                    )

                    when (packet.protocolNumber) {
                        0x01 -> {
                            val imei = Pt20LoginDecoder.decodeImei(packet.content)
                            log.info("PT20 login received, IMEI={}", imei)

                            val resolvedId =
                                deviceJsonRpcClient.getDevicePublicByImei(imei)

                            devicePublicId = resolvedId

                            log.info(
                                "Device resolved successfully, publicId={}",
                                resolvedId
                            )

                            // Standard GT06-style login ACK.
                            // Verify this format against the PT20 specification.
                            sendAck(output, packet.protocolNumber, packet.serialNumber)
                        }

                        0x22 -> {
                            val registeredDeviceId = devicePublicId
                                ?: throw IllegalStateException(
                                    "GPS packet received before device login"
                                )

                            val location = gpsLocationDecoder.decode(packet)

                            log.info(
                                "PT20 GPS location: lat={}, lon={}, speed={}, fixed={}",
                                location.latitude,
                                location.longitude,
                                location.speed,
                                location.gpsFixed
                            )

                            locationService.saveLocation(
                                LocationRequest(
                                    devicePublicId = registeredDeviceId,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    speed = location.speed.toDouble(),
                                    gpsTimestamp = location.gpsTimestamp
                                )
                            )

                            log.info(
                                "GPS location saved for device={}",
                                registeredDeviceId
                            )

                            // Standard GT06-style GPS packet ACK.
                            // Verify this format against the PT20 specification.
                            sendAck(output, packet.protocolNumber, packet.serialNumber)
                        }

                        else -> {
                            log.info(
                                "PT20 non-location packet received: protocol=0x{}",
                                "%02X".format(packet.protocolNumber)
                            )
                        }
                    }
                }
            } catch (ex: SocketTimeoutException) {
                log.warn(
                    "PT20 read timed out; remote={}, device={}",
                    client.remoteSocketAddress,
                    devicePublicId
                )
            } catch (ex: SocketException) {
                log.info(
                    "PT20 socket closed: remote={}, reason={}",
                    client.remoteSocketAddress,
                    ex.message
                )
            } catch (ex: Exception) {
                log.error(
                    "PT20 client processing failed: remote={}",
                    client.remoteSocketAddress,
                    ex
                )
            } finally {
                log.info("PT20 client connection closed: {}", client.remoteSocketAddress)
            }
        }
    }

    private fun sendAck(
        output: java.io.OutputStream,
        protocolNumber: Int,
        serialNumber: Int
    ) {
        val frame = byteArrayOf(
            0x78,
            0x78,
            0x05,
            protocolNumber.toByte(),
            ((serialNumber shr 8) and 0xFF).toByte(),
            (serialNumber and 0xFF).toByte(),
            0x00,
            0x00,
            0x0D,
            0x0A
        )

        // CRC covers length, protocol number and serial number.
        val crc = calculateCrc16(frame, 2, 6)

        frame[6] = ((crc shr 8) and 0xFF).toByte()
        frame[7] = (crc and 0xFF).toByte()

        output.write(frame)
        output.flush()

        log.info(
            "PT20 ACK sent: protocol=0x{}, serial={}, bytes={}",
            "%02X".format(protocolNumber),
            serialNumber,
            frame.toHex()
        )
    }

    private fun calculateCrc16(
        data: ByteArray,
        start: Int,
        endExclusive: Int
    ): Int {
        var fcs = 0xFFFF

        for (index in start until endExclusive) {
            fcs = fcs xor (data[index].toInt() and 0xFF)

            repeat(8) {
                fcs = if ((fcs and 1) != 0) {
                    (fcs ushr 1) xor 0x8408
                } else {
                    fcs ushr 1
                }
            }
        }

        return fcs.inv() and 0xFFFF
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    @PreDestroy
    fun stop() {
        running.set(false)

        try {
            serverSocket?.close()
        } catch (ex: Exception) {
            log.debug("Error closing PT20 server socket", ex)
        }

        clients.shutdownNow()
        serverThread?.interrupt()
    }
}
