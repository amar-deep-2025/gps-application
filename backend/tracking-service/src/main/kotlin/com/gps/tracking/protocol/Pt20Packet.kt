
package com.gps.tracking.protocol

data class Pt20Packet(
    val protocolNumber: Int,
    val content: ByteArray,
    val serialNumber: Int,
    val crc: Int,
    val rawPacket: ByteArray
)

class Pt20PacketDecoder {

    fun decode(packet: ByteArray): Pt20Packet
    {
        require(packet.size >= 10) {
            "PT20 packet is too short"
        }

        val isShortFrame =
            packet[0] == 0x78.toByte() &&
                    packet[1] == 0x78.toByte()

        val isExtendedFrame =
            packet[0] == 0x79.toByte() &&
                    packet[1] == 0x79.toByte()

        require(isShortFrame || isExtendedFrame) {
            "Invalid PT20 start bits"
        }

        require(
            packet[packet.size - 2] == 0x0D.toByte() &&
                    packet[packet.size - 1] == 0x0A.toByte()
        ) {
            "Invalid PT20 stop bits"
        }

        val length: Int
        val protocolIndex: Int

        if (isShortFrame) {
            length = packet[2].toInt() and 0xFF
            protocolIndex = 3
        } else {
            length =
                ((packet[2].toInt() and 0xFF) shl 8) or
                        (packet[3].toInt() and 0xFF)

            protocolIndex = 4
        }

        val expectedSize =
            if (isShortFrame) length + 5 else length + 6

        require(packet.size == expectedSize) {
            "Packet length mismatch: expected $expectedSize, received ${packet.size}"
        }

        // Last 6 bytes: serial (2), CRC (2), stop bits (2)
        val serialIndex = packet.size - 6
        val crcIndex = packet.size - 4

        val protocolNumber =
            packet[protocolIndex].toInt() and 0xFF

        val serialNumber =
            ((packet[serialIndex].toInt() and 0xFF) shl 8) or
                    (packet[serialIndex + 1].toInt() and 0xFF)

        val crc =
            ((packet[crcIndex].toInt() and 0xFF) shl 8) or
                    (packet[crcIndex + 1].toInt() and 0xFF)

        val content = packet.copyOfRange(
            protocolIndex + 1,
            serialIndex
        )


        val calculatedCrc=calculateCRc116(
            packet,
            start=2,
            endExclusive=serialIndex+2
        )

        require(crc==calculatedCrc){
            "Invalid PT20 CRC: expected %04X, received %04X".format(calculatedCrc,crc)
        }

        return Pt20Packet(
            protocolNumber = protocolNumber,
            content = content,
            serialNumber = serialNumber,
            crc = crc,
            rawPacket = packet.copyOf()
        )
    }
    private fun calculateCRc116(
        data:ByteArray,
        start:Int,
        endExclusive:Int
    ):Int{
        var fcs= 0xFFFF
        for (index in start until endExclusive){
            fcs=fcs xor(data[index].toInt() and 0xFF)

            repeat(8){
                fcs=if ((fcs and 1)!=0){
                    (fcs ushr 1) xor 0x8408
                }else{
                    fcs ushr 1
                }
            }
        }
        return fcs.inv() and 0xFFFF
    }
}
