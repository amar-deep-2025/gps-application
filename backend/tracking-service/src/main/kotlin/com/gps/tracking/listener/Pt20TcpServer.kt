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
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    private val heartbeatDecoder = Pt20HeartbeatDecoder()
    private val running = AtomicBoolean(false)

    // Bound fixed thread pool to prevent system out of memory runtime issues
    private val clients: ExecutorService = Executors.newFixedThreadPool(200) { runnable ->
        Thread(runnable).apply { isDaemon = true }
    }

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
                    log.info("PT20 TCP server engine listening on port {}", port)

                    while (running.get()) {
                        val socket = server.accept()
                        log.info("New tracker channel link established from: {}", socket.remoteSocketAddress)
                        clients.submit { handleClient(socket) }
                    }
                }
            } catch (ex: SocketException) {
                if (running.get()) {
                    log.error("PT20 TCP server socket connection error encountered", ex)
                }
            } catch (ex: Exception) {
                if (running.get()) {
                    log.error("PT20 TCP server processing loop failed", ex)
                }
            }
        }, "pt20-tcp-server").apply {
            isDaemon = true
            start()
        }
    }

    @PreDestroy
    fun stop() {
        if (!running.compareAndSet(true, false)) return
        log.info("Initiating graceful shutdown sequence for PT20 TCP server container platform...")

        try {
            serverSocket?.close()
        } catch (ex: Exception) {
            log.error("Failed to safely close the server socket boundary context link", ex)
        }

        clients.shutdown()
        try {
            if (!clients.awaitTermination(5, TimeUnit.SECONDS)) {
                clients.shutdownNow()
            }
        } catch (ex: InterruptedException) {
            clients.shutdownNow()
            Thread.currentThread().interrupt()
        }
        log.info("PT20 TCP engine stopped completely.")
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            var devicePublicId: UUID? = null
            var deviceImei: String? = null
            try {
                client.soTimeout = 120_000 // Standard 2-minute connection timeout threshold
                val input = client.getInputStream()
                val output = client.getOutputStream()
                val packet = ByteArrayOutputStream()
                var previous = -1

                while (running.get()) {
                    val current = input.read()
                    if (current == -1) break

                    packet.write(current)

                    // PT20 packet terminator signature detection match: 0D 0A
                    if (previous == 0x0D && current == 0x0A) {
                        val bytes = packet.toByteArray()
                        packet.reset()
                        previous = -1 // Reset historical streaming markers early

                        try {
                            val decodedPacket = packetDecoder.decode(bytes)
                            log.info("PT20 packet decoded: protocol = 0x{}, serial={}", "%02x".format(decodedPacket.protocolNumber), decodedPacket.serialNumber)

                            // Explicit bit extraction array check to match protocol types safely
                            val isStandardFrame = bytes[0] == 0x78.toByte() && bytes[1] == 0x78.toByte()

                            when (decodedPacket.protocolNumber) {
                                0x01 -> { // LOGIN PACKET HANDSHAKE
                                    deviceImei = Pt20LoginDecoder.decodeImei(decodedPacket.content)
                                    log.info("PT20 login packet received, IMEI={}", deviceImei)
                                    devicePublicId = deviceJsonRpcClient.getDevicePublicByImei(deviceImei)
                                    log.info("Device resolved successfully, publicId={}", devicePublicId)

                                    // DELIVER LOGIN CONFIRMATION IMMEDIATE REPLY
                                    sendReply(output, decodedPacket.protocolNumber, decodedPacket.serialNumber, isStandardFrame)
                                    log.info("Login handshake confirmation response successfully written downstream for IMEI: {}", deviceImei)
                                }

                                0x22 -> { // GPS TRACKING METRICS LOCATION PAYLOAD
                                    val location = gpsLocationDecoder.decode(decodedPacket)
                                    log.info("PT20 GPS location data matched: latitude={}, longitude={}, speed={} km/h", location.latitude, location.longitude, location.speed)

                                    val registeredDeviceId = devicePublicId ?: throw IllegalStateException("GPS packet received before device login structural handshake sequence")

                                    locationService.saveLocation(
                                        LocationRequest(
                                            devicePublicId = registeredDeviceId,
                                            latitude = location.latitude,
                                            longitude = location.longitude,
                                            speed = location.speed.toDouble(),
                                            gpsTimestamp = location.gpsTimestamp
                                        )
                                    )
                                    log.info("GPS location metrics records saved successfully for device={}", registeredDeviceId)
                                }
                                0x13 -> { // HARDWARE COMPONENT SYSTEM STATE HEARTBEAT
                                    // 1. Direct call to unified alerts class
                                    val heartbeatAlert = heartbeatDecoder.decodeHeartbeatAlert(decodedPacket)

                                    val registeredDevice = devicePublicId ?: throw IllegalStateException("Heartbeat tracking drop: Session has not performed active channel registration actions")
                                    val imei = deviceImei ?: throw IllegalStateException("Identity map metrics trace data field corrupted or missing inside pool indexes")

                                    deviceJsonRpcClient.updateLastSeenAt(imei)

                                    // Complete structural logs with standard string messages
                                    log.info("PT20 heartbeat processed successfully for device mapping container context: {} " +
                                            "alertTag={}, userMsg={}, isCritical={}",
                                        registeredDevice, heartbeatAlert.activeAlertName, heartbeatAlert.userFriendlyMessage, heartbeatAlert.isCritical)

                                    // DELIVER HEARTBEAT HANDSHAKE STATUS RESPONSE
                                    sendReply(output, decodedPacket.protocolNumber, decodedPacket.serialNumber, isStandardFrame)
                                    log.info("Heartbeat loop state synchronization confirmation successfully written back downstream to tracker terminal.")
                                }

                                0x26 -> { // REAL-TIME LIVE DEDICATED EMERGENCY ALARM
                                    // 2. Direct call to dedicated alarms mapping
                                    val criticalAlarm = heartbeatDecoder.decodeDedicatedAlarm(decodedPacket)

                                    val registeredDevice = devicePublicId ?: throw IllegalStateException("Alarm tracking drop: Session has not performed active channel registration actions")
                                    val imei = deviceImei ?: throw IllegalStateException("Identity map metrics trace data field corrupted or missing inside pool indexes")

                                    deviceJsonRpcClient.updateLastSeenAt(imei)

                                    log.info("PT20 critical alarm processed successfully for device mapping container context: {} " +
                                            "alertTag={}, userMsg={}, isCritical={}",
                                        registeredDevice, criticalAlarm.activeAlertName, criticalAlarm.userFriendlyMessage, criticalAlarm.isCritical)

                                    // DELIVER EMERGENCY ACKNOWLEDGEMENT RESPONSE
                                    sendReply(output, decodedPacket.protocolNumber, decodedPacket.serialNumber, isStandardFrame)
                                    log.info("Emergency alarm verification frame successfully written back downstream to tracker terminal.")
                                }
                                else -> {
                                    log.info("PT20 custom data packet target path execution branch triggered: protocol=0x{}", "%02X".format(decodedPacket.protocolNumber))
                                    log.info(
                                        "PT20 RAW PACKET: {}",
                                        decodedPacket.rawPacket.joinToString(" ") {
                                            "%02X".format(it.toInt() and 0xFF)
                                        }
                                    )

                                    log.info(
                                        "PT20 CONTENT: {}",
                                        decodedPacket.content.joinToString(" ") {
                                            "%02X".format(it.toInt() and 0xFF)
                                        }
                                    )
                                }
                            }
                        } catch (ex: IllegalArgumentException) {
                            log.warn("Invalid corrupted PT20 stream frame layout dropped: {}", ex.message)
                        } catch (ex: Exception) {
                            log.error("Internal processing error inside client connection payload loop parameters", ex)
                        }
                        continue
                    }
                    previous = current
                }
            } catch (ex: SocketException) {
                log.info("Connection cleanly dropped or reset by client terminal tracking hardware node: {}", ex.message)
            } catch (ex: Exception) {
                log.error("Critical execution breakdown within the active socket pipeline worker processing sequence threads", ex)
            }
        }
    }

    /**
     * Standard response serialization layer builder tracking downstream validation constraints.
     */
    private fun sendReply(stream: OutputStream, protocolNumber: Int, serialNumber: Int, isStandardFrame: Boolean) {
        val payloadStream = ByteArrayOutputStream()

        if (isStandardFrame) {
            payloadStream.write(0x78)
            payloadStream.write(0x78)
            payloadStream.write(0x05) // Length: Protocol (1) + Serial (2) + CRC (2) -> Standard explicit metadata length index indicator
        } else {payloadStream.write(0x79)
            payloadStream.write(0x79)
            payloadStream.write(0x00)
            payloadStream.write(0x05)
        }
        payloadStream.write(protocolNumber)
        payloadStream.write((serialNumber shr 8) and 0xFF)
        payloadStream.write(serialNumber and 0xFF)
        val responseBufferArray = payloadStream.toByteArray()
// FIXED COMPUTATION BOUNDS: Skip first 2 bytes (Start Bits), compute over the exact remaining structural frame footprint width block
        val computedValidationChecksum = calculateCrc16CcittFalse(responseBufferArray, 2, responseBufferArray.size)
        payloadStream.write((computedValidationChecksum shr 8) and 0xFF)
        payloadStream.write(computedValidationChecksum and 0xFF)
        payloadStream.write(0x0D)
        payloadStream.write(0x0A)
        stream.write(payloadStream.toByteArray())
        stream.flush()
    }
    private fun calculateCrc16CcittFalse(data: ByteArray, start: Int, endExclusive: Int): Int {
        var crc = 0xFFFF
        for (index in start until endExclusive) {
            val byteVal = data[index].toInt() and 0xFF
            crc = crc xor (byteVal shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                }
            }
        }
        return crc and 0xFFFF
    }
}
