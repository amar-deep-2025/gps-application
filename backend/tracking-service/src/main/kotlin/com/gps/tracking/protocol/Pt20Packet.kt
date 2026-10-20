package com.gps.tracking.protocol

import java.util.logging.Logger

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

    companion object {
        private val logger = Logger.getLogger(Pt20PacketDecoder::class.java.name)
    }

    fun decode(packet: ByteArray): Pt20Packet {
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

        val expectedSize = if (isShortFrame) {
            length + 5
        } else {
            length + 6
        }

        require(packet.size == expectedSize) {
            "Packet length mismatch: expected $expectedSize, received ${packet.size}"
        }

        val serialIndex = packet.size - 6
        val crcIndex = packet.size - 4

        require(protocolIndex < serialIndex) {
            "Invalid PT20 packet structure"
        }

        val protocolNumber =
            packet[protocolIndex].toInt() and 0xFF

        val serialNumber =
            ((packet[serialIndex].toInt() and 0xFF) shl 8) or
                    (packet[serialIndex + 1].toInt() and 0xFF)

        val receivedCrc =
            ((packet[crcIndex].toInt() and 0xFF) shl 8) or
                    (packet[crcIndex + 1].toInt() and 0xFF)

        val content = packet.copyOfRange(
            protocolIndex + 1,
            serialIndex
        )

        // Using your original verified CRC range calculation
        val calculatedCrc = calculateCrcItu(
            data = packet,
            start = 2,
            endExclusive = serialIndex + 2
        )

        require(receivedCrc == calculatedCrc) {
            "Invalid PT20 CRC: calculated %04X, received %04X".format(
                calculatedCrc,
                receivedCrc
            )
        }

        logger.fine(
            "PT20 packet decoded: protocol=0x%02X, serial=%d, payloadBytes=%d"
                .format(protocolNumber, serialNumber, content.size)
        )

        return Pt20Packet(
            protocolNumber = protocolNumber,
            content = content,
            serialNumber = serialNumber,
            crc = receivedCrc,
            rawPacket = packet.copyOf()
        )
    }

    // Restored back to your working X.25 / Reverse CCITT algorithm
    private fun calculateCrcItu(
        data: ByteArray,
        start: Int,
        endExclusive: Int
    ): Int {
        require(start >= 0 && endExclusive <= data.size && start <= endExclusive) {
            "Invalid CRC range"
        }

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
