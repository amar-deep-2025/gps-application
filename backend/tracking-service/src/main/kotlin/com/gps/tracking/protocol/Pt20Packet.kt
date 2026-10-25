package com.gps.tracking.protocol

data class Pt20Packet(
    val protocolNumber: Int,
    val content: ByteArray,
    val serialNumber: Int,
    val crc: Int,
    val rawPacket: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Pt20Packet
        if (protocolNumber != other.protocolNumber) return false
        if (!content.contentEquals(other.content)) return false
        if (serialNumber != other.serialNumber) return false
        if (crc != other.crc) return false
        if (!rawPacket.contentEquals(other.rawPacket)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = protocolNumber
        result = 31 * result + content.contentHashCode()
        result = 31 * result + serialNumber
        result = 31 * result + crc
        result = 31 * result + rawPacket.contentHashCode()
        return result
    }
}

class Pt20PacketDecoder {

    fun decode(packet: ByteArray): Pt20Packet {
        require(packet.size >= 10) { "PT20 packet is too short" }

        val isShortFrame = packet[0] == 0x78.toByte() && packet[1] == 0x78.toByte()
        val isExtendedFrame = packet[0] == 0x79.toByte() && packet[1] == 0x79.toByte()

        require(isShortFrame || isExtendedFrame) { "Invalid PT20 start bits" }

        require(
            packet[packet.size - 2] == 0x0D.toByte() &&
                    packet[packet.size - 1] == 0x0A.toByte()
        ) { "Invalid PT20 stop bits" }

        val length: Int
        val protocolIndex: Int

        if (isShortFrame) {
            length = packet[2].toInt() and 0xFF
            protocolIndex = 3
        } else {
            length = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
            protocolIndex = 4
        }

        val expectedSize = if (isShortFrame) length + 5 else length + 6
        require(packet.size == expectedSize) {
            "Packet length mismatch: expected $expectedSize, received ${packet.size}"
        }

        val serialIndex = packet.size - 6
        val crcIndex = packet.size - 4

        val protocolNumber = packet[protocolIndex].toInt() and 0xFF
        val serialNumber = ((packet[serialIndex].toInt() and 0xFF) shl 8) or (packet[serialIndex + 1].toInt() and 0xFF)
        val receivedCrc = ((packet[crcIndex].toInt() and 0xFF) shl 8) or (packet[crcIndex + 1].toInt() and 0xFF)

        val content = packet.copyOfRange(protocolIndex + 1, serialIndex)

        // FIXED INDEX: The calculation range must encompass everything from length up to right before the CRC field
        val startOffset = 2
        val endOffsetExclusive = crcIndex

        val computedCcittFalse = calculateCrc16CcittFalse(packet, startOffset, endOffsetExclusive)
        val computedX25 = calculateCrc16X25(packet, startOffset, endOffsetExclusive)

        // Self-Healing Strategy: Accept either variation to support both standard and white-label hardware variants smoothly
        val finalValidatedCrc = when (receivedCrc) {
            computedCcittFalse -> computedCcittFalse
            computedX25 -> computedX25
            else -> throw IllegalArgumentException(
                "Invalid PT20 CRC: received %04X (Server expected CCITT-FALSE: %04X or X-25: %04X)"
                    .format(receivedCrc, computedCcittFalse, computedX25)
            )
        }

        return Pt20Packet(
            protocolNumber = protocolNumber,
            content = content,
            serialNumber = serialNumber,
            crc = finalValidatedCrc,
            rawPacket = packet.copyOf()
        )
    }

    /**
     * Standard Left-Shifting CRC-16/CCITT-FALSE
     */
    private fun calculateCrc16CcittFalse(data: ByteArray, start: Int, endExclusive: Int): Int {
        var crc = 0xFFFF
        for (index in start until endExclusive) {
            val byteVal = data[index].toInt() and 0xFF
            crc = crc xor (byteVal shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) (crc shl 1) xor 0x1021 else crc shl 1
            }
        }
        return crc and 0xFFFF
    }

    /**
     * Alternative Right-Shifting CRC-16/X-25 (Used by select PT20 hardware variants)
     */
    private fun calculateCrc16X25(data: ByteArray, start: Int, endExclusive: Int): Int {
        var fcs = 0xFFFF
        for (index in start until endExclusive) {
            fcs = fcs xor (data[index].toInt() and 0xFF)
            repeat(8) {
                fcs = if ((fcs and 1) != 0) (fcs ushr 1) xor 0x8408 else fcs ushr 1
            }
        }
        return fcs.inv() and 0xFFFF
    }
}
