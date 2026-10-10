package com.gps.tracking.protocol

import org.testng.Assert.assertThrows
import org.testng.annotations.Test

class Pt20PacketDecoderTest {

    private val decoder=Pt20PacketDecoder()

    @Test
    fun `should reject packet with invalid start bits`(){
        val packet=ByteArray(10)
        packet[0]=0x00
        packet[1]=0x00

        assertThrows(IllegalArgumentException::class.java){
            decoder.decode(packet)
        }
    }

    @Test
    fun `should reject packet with invalid CRC`(){
        val packet= byteArrayOf(
            0x78, 0x78, 0x05,
            0x01,
            0x00, 0x01,
            0x00, 0x00,
            0x0D, 0x0A
        )
        assertThrows(IllegalArgumentException::class.java){
            decoder.decode(packet)
        }
    }
    @Test
    fun `should reject packet with invalid stop bits`() {
        val packet = byteArrayOf(
            0x78, 0x78, 0x05, 0x01,
            0x00, 0x01, 0x00, 0x00,
            0x00, 0x00
        )

        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }
    @Test
    fun `should reject packet with incorrect length`() {
        val packet = byteArrayOf(
            0x78, 0x78, 0x06, // Incorrect length field
            0x01,
            0x00, 0x01,
            0x00, 0x00,
            0x0D, 0x0A
        )

        assertThrows(IllegalArgumentException::class.java) {
            decoder.decode(packet)
        }
    }
}