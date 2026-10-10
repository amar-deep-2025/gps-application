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
}