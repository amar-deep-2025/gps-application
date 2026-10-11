
package com.gps.tracking.protocol

import java.net.Socket

object Pt20TcpTestClient {

    @JvmStatic
    fun main(args: Array<String>) {
        val content = ByteArray(18)

        // UTC timestamp: 2026-10-11 10:30:45
        content[0] = 26
        content[1] = 10
        content[2] = 11
        content[3] = 10
        content[4] = 30
        content[5] = 45

        // GPS information: 11 satellites
        content[6] = 0xCB.toByte()

        // Latitude: 28.6139
        putUnsignedInt(
            content, 7,
            (28.6139 * 1_800_000).toLong()
        )

        // Longitude: 77.2090
        putUnsignedInt(
            content, 11,
            (77.2090 * 1_800_000).toLong()
        )

        // Speed: 40 km/h
        content[15] = 40

        // North + East flags, heading = 90 degrees
        content[16] = 0x0C
        content[17] = 90

        val packet = buildPacket(content, serialNumber = 1)

        println("Sending PT20 test packet:")
        println(
            packet.joinToString(" ") {
                "%02X".format(it.toInt() and 0xFF)
            }
        )

        Socket("127.0.0.1", 5054).use { socket ->
            socket.getOutputStream().apply {
                write(packet)
                flush()
            }
        }

        println("Test packet sent successfully.")
    }

    private fun putUnsignedInt(
        target: ByteArray,
        offset: Int,
        value: Long
    ) {
        require(value in 0..0xFFFF_FFFFL)

        for (i in 0 until 4) {
            val shift = (3 - i) * 8
            target[offset + i] = (value shr shift).toByte()
        }
    }

    private fun buildPacket(
        content: ByteArray,
        serialNumber: Int
    ): ByteArray {
        require(serialNumber in 0..0xFFFF)

        // Protocol + content + serial + CRC
        val length = 1 + content.size + 2 + 2
        require(length <= 0xFF)

        val packet = ByteArray(length + 5)

        packet[0] = 0x78
        packet[1] = 0x78
        packet[2] = length.toByte()
        packet[3] = 0x22

        content.copyInto(packet, destinationOffset = 4)

        val serialIndex = packet.size - 6

        packet[serialIndex] = (serialNumber shr 8).toByte()
        packet[serialIndex + 1] = serialNumber.toByte()

        val crc = calculateCrc(
            packet,
            start = 2,
            endExclusive = serialIndex + 2
        )

        packet[serialIndex + 2] = (crc shr 8).toByte()
        packet[serialIndex + 3] = crc.toByte()

        packet[packet.size - 2] = 0x0D
        packet[packet.size - 1] = 0x0A

        return packet
    }

    private fun calculateCrc(
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
}
