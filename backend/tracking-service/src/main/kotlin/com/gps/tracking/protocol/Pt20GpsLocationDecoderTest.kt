package com.gps.tracking.protocol

import org.testng.Assert.assertThrows
import org.testng.annotations.Test

class Pt20GpsLocationDecoderTest {

    private val decoder=Pt20GpsLocationDecoder()

    @Test
    fun `should reject unsupported protocol number`(){
        val packet=Pt20Packet(
            protocolNumber = 0x01,
            content= byteArrayOf(),
            serialNumber = 1,
            crc=0,
            rawPacket = byteArrayOf()
        )
        assertThrows(IllegalArgumentException::class.java){
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
}