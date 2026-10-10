
package com.gps.tracking.protocol

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Component
class Pt20TcpServer(
    @Value("\${tracking.tcp.port:5054}")
    private val port: Int
) {
    private val log = LoggerFactory.getLogger(Pt20TcpServer::class.java)
    private val running = AtomicBoolean(false)
    private val clients: ExecutorService =
        Executors.newCachedThreadPool()

    @Volatile
    private var serverSocket: ServerSocket? = null

    private var serverThread: Thread? = null

    @jakarta.annotation.PostConstruct
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
            try {
                client.soTimeout = 120_000
                val input = client.getInputStream()
                val packet = ByteArrayOutputStream()
                var previous = -1

                while (running.get()) {
                    val current = input.read()
                    if (current == -1) break

                    packet.write(current)

                    // PT20 packet terminator: 0D 0A
                    if (previous == 0x0D && current == 0x0A) {
                        val bytes = packet.toByteArray()
                        log.info(
                            "PT20 raw packet from {}: {}",
                            client.remoteSocketAddress,
                            bytes.joinToString(" ") {
                                "%02X".format(it.toInt() and 0xFF)
                            }
                        )
                        packet.reset()
                    }

                    previous = current
                }
            } catch (ex: Exception) {
                if (running.get()) {
                    log.warn(
                        "PT20 client disconnected/error: {}",
                        ex.message
                    )
                }
            }
        }
    }

    @PreDestroy
    fun stop() {
        running.set(false)
        serverSocket?.close()
        clients.shutdownNow()
        serverThread?.interrupt()
    }
}
