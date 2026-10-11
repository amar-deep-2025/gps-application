
package com.gps.tracking.protocol

import java.net.Socket

object Pt20TcpTestClient {


    @JvmStatic
    fun main(args: Array<String>) {
        val terminalId = "867648060123456"

        // PT20 login packet: Terminal ID = 8 bytes
        val loginContent = ByteArray(12)

        val imeiHex = terminalId
            .padStart(16, '0')
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

        imeiHex.copyInto(loginContent, destinationOffset = 0)

        // Terminal type identifier
        loginContent[8] = 0x00
        loginContent[9] = 0x01

        // Time zone/language: GMT+8, English
        loginContent[10] = 0x32
        loginContent[11] = 0x02

        val loginPacket = buildPacket(loginContent, serialNumber = 1).also {
            it[3] = 0x01
            val serialIndex = it.size - 6
            val crc = calculateCrc(it, 2, serialIndex + 2)
            it[serialIndex + 2] = (crc shr 8).toByte()
            it[serialIndex + 3] = crc.toByte()
        }


        Socket("127.0.0.1", 5054).use { socket ->
            val output = socket.getOutputStream()

            // Send login packet first
            output.write(loginPacket)
            output.flush()
            println("PT20 login test packet sent.")

            // GPS packet content: 18 bytes
            val gpsContent = ByteArray(18)

            // Timestamp: 2026-10-11 10:30:45 UTC
            gpsContent[0] = 26
            gpsContent[1] = 10
            gpsContent[2] = 11
            gpsContent[3] = 10
            gpsContent[4] = 30
            gpsContent[5] = 45

            // GPS information: 11 satellites
            gpsContent[6] = 0xCB.toByte()

            // Latitude: 28.6139
            putUnsignedInt(
                gpsContent, 7,
                (28.6139 * 1_800_000).toLong()
            )

            // Longitude: 77.2090
            putUnsignedInt(
                gpsContent, 11,
                (77.2090 * 1_800_000).toLong()
            )

            // Speed: 40 km/h
            gpsContent[15] = 40

            // Course/state: heading 90°, North + East flags
            gpsContent[16] = 0x1C
            gpsContent[17] = 0x5A

            val gpsPacket = buildPacket(
                gpsContent,
                serialNumber = 2
            )

            output.write(gpsPacket)
            output.flush()

            println("PT20 GPS test packet sent.")
        }
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
