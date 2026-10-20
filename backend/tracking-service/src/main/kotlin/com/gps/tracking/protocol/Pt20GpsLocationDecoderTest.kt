package com.gps.tracking.protocol

import org.testng.Assert.assertThrows
import org.testng.AssertJUnit.assertEquals
import org.testng.annotations.Test

class Pt20GpsLocationDecoderTest {

    private val decoder = Pt20GpsLocationDecoder()

    @Test
    fun `should reject unsupported protocol number`() {
        val packet = Pt20Packet(
            protocolNumber = 0x01,
            content = byteArrayOf(),
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )
        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }

    @Test
    fun `should reject empty GPS payload`() {
        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = byteArrayOf(),
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )
        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }

    @Test
    fun `should reject payload shorter than timestamp`() {
        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = byteArrayOf(26, 10, 9, 12, 30),
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )
        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }

    @Test
    fun `should decode GPS timestamp and location fields`() {
        val content = ByteArray(29)

        // Date and Time: 2026-10-09 12:30:45
        content[0] = 26
        content[1] = 10
        content[2] = 9
        content[3] = 12
        content[4] = 30
        content[5] = 45

        // Gps info byte: Satellites calculation mask
        content[6] = 0xCB.toByte() // 0xCB and 0x0F = 11 Satellites

        // Latitude raw value: 22.545 * 1800000
        val latitudeRaw = (22.545 * 1_800_000).toLong()
        content[7] = (latitudeRaw shr 24).toByte()
        content[8] = (latitudeRaw shr 16).toByte()
        content[9] = (latitudeRaw shr 8).toByte()
        content[10] = latitudeRaw.toByte()

        // Longitude raw value: 77.0 * 1800000
        val longitudeRaw = (77.0 * 1_800_000).toLong()
        content[11] = (longitudeRaw shr 24).toByte()
        content[12] = (longitudeRaw shr 16).toByte()
        content[13] = (longitudeRaw shr 8).toByte()
        content[14] = longitudeRaw.toByte()

        content[15] = 40 // Speed: 40 km/h

        // Course & Status flags array configuration:
        // Heading standard 90 degrees = 0x005A (bit structure)
        // Bit flags for direction: North (0x0400) + East (0x0800) = 0x0C00
        // Combined Integer (courseState) = 0x0C00 or 0x005A = 0x0C5A
        content[16] = 0x0C.toByte() // High Byte flag bits
        content[17] = 0x5A.toByte() // Low Byte heading info bits

        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = content,
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )
        val result = decoder.decode(packet)

        // Verification Checks
        assertEquals(result.satelliteCount, 11)
        assertEquals(result.speed, 40)
        assertEquals(result.heading, 90)
        assertEquals(result.gpsFixed, true) // Bit 0x1000 is 0, so gpsFixed should be true
        assertEquals(result.latitude, 22.545, 0.001)
        assertEquals(result.longitude, 77.0, 0.001)
        assertEquals(result.gpsTimestamp.toString(), "2026-10-09T12:30:45Z")
    }

    @Test
    fun `should reject invalid GPS timestamp`() {
        val content = ByteArray(29)
        content[0] = 26
        content[1] = 13 // Invalid month value (13)
        content[2] = 9
        content[3] = 12
        content[4] = 30
        content[5] = 45
        content[6] = 0xCB.toByte()

        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = content,
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }

    @Test
    fun `should reject incomplete GPS location payload`() {
        val content = ByteArray(17) // Location properties block missing completely

        content[0] = 26
        content[1] = 10
        content[2] = 9
        content[3] = 12
        content[4] = 30
        content[5] = 45
        content[6] = 0xCB.toByte()

        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = content,
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }

    @Test
    fun `should reject payload without GPS info byte`() {
        val content = ByteArray(6) // Only contains timestamp block size

        val packet = Pt20Packet(
            protocolNumber = 0x22,
            content = content,
            serialNumber = 1,
            crc = 0,
            rawPacket = byteArrayOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }
}
