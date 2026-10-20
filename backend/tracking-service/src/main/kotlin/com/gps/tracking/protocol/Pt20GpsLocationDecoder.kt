package com.gps.tracking.protocol

import com.gps.tracking.protocol.dto.Pt20GpsLocation
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.logging.Logger

class Pt20GpsLocationDecoder {

    companion object {
        private val logger =
            Logger.getLogger(Pt20GpsLocationDecoder::class.java.name)
    }

    fun decode(packet: Pt20Packet): Pt20GpsLocation {
        require(packet.protocolNumber == 0x22) {
            "Unsupported PT20 protocol: ${packet.protocolNumber}"
        }

        val content = packet.content

        require(content.size >= 18) {
            "PT20 GPS payload must contain timestamp, coordinates, speed and course/status"
        }

        // 1. Decode UTC timestamp.
        val year = content[0].toInt() and 0xFF
        val month = content[1].toInt() and 0xFF
        val day = content[2].toInt() and 0xFF
        val hour = content[3].toInt() and 0xFF
        val minute = content[4].toInt() and 0xFF
        val second = content[5].toInt() and 0xFF

        require(
            month in 1..12 &&
                    day in 1..31 &&
                    hour in 0..23 &&
                    minute in 0..59 &&
                    second in 0..59
        ) {
            "Invalid PT20 GPS timestamp values: " +
                    "$year-$month-$day $hour:$minute:$second"
        }
        val decodedYear=2000+year
        val gpsTimestamp = try {
            if (decodedYear<2025){
                java.time.Instant.now()
            }else {
                LocalDateTime.of(
                    decodedYear,
                    month,
                    day,
                    hour,
                    minute,
                    second
                ).toInstant(ZoneOffset.UTC)
            }
        } catch (ex: DateTimeException) {
            throw IllegalArgumentException(
                "Invalid PT20 GPS timestamp",
                ex
            )
        }

        // 2. GPS information byte.
        val gpsInfo = content[6].toInt() and 0xFF
        val satelliteCount = gpsInfo and 0x0F

        // 3. Decode unsigned 32-bit latitude and longitude.
        val rawLatitude = readUnsignedInt(content, 7)
        val rawLongitude = readUnsignedInt(content, 11)

        // PT20 coordinate conversion: raw value / 1,800,000.
        val latitudeMagnitude = rawLatitude / 1_800_000.0
        val longitudeMagnitude = rawLongitude / 1_800_000.0

        require(latitudeMagnitude <= 90.0) {
            "Invalid PT20 latitude magnitude: $latitudeMagnitude"
        }

        require(longitudeMagnitude <= 180.0) {
            "Invalid PT20 longitude magnitude: $longitudeMagnitude"
        }

        // 4. Speed in km/h as specified by the device protocol.
        val speed = content[15].toInt() and 0xFF

        // 5. Course/status is encoded in two bytes.
        val statusHigh = content[16].toInt() and 0xFF
        val statusLow = content[17].toInt() and 0xFF

        val courseState = (statusHigh shl 8) or statusLow

        // Lower 10 bits represent heading.
        val heading = courseState and 0x03FF

        // Standard GT06/PT20 Protocol Direction/Fix Flags Corrected Logic:
        // Bit 12: 1 = GPS is fixed and has active satellite lock.
        val gpsFixed = (courseState and 0x1000) != 0

        // Bit 11: 0 = East, 1 = West (Yahan 0 check true return karega India region ke liye).
        val longitudeEast = (courseState and 0x0800)== 0

        // Bit 10: 0 = North, 1 = South (Yahan 0 check true return karega North Hemisphere ke liye).
        val latitudeNorth = (courseState and 0x0400)!= 0

        val finalLatitude = if (latitudeNorth) {
            latitudeMagnitude
        } else {
            -latitudeMagnitude
        }

        val finalLongitude = if (longitudeEast) {
            longitudeMagnitude
        } else {
            -longitudeMagnitude
        }

        // 6. Diagnostics for comparing the TCP packet with the device SMS.
        logger.info(
            """
            PT20 GPS decoded:
            serialNumber=${packet.serialNumber}
            gpsTimestampUtc=$gpsTimestamp
            gpsInfo=0x%02X
            satelliteCount=$satelliteCount
            rawLatitude=$rawLatitude
            rawLongitude=$rawLongitude
            statusHigh=0x%02X
            statusLow=0x%02X
            courseState=0x%04X
            longitudeEast=$longitudeEast
            latitudeNorth=$latitudeNorth
            gpsFixed=$gpsFixed
            latitude=$finalLatitude
            longitude=$finalLongitude
            speed=$speed
            heading=$heading
            """.trimIndent().format(
                gpsInfo,
                statusHigh,
                statusLow,
                courseState
            )
        )

        return Pt20GpsLocation(
            latitude = finalLatitude,
            longitude = finalLongitude,
            speed = speed,
            heading = heading,
            gpsTimestamp = gpsTimestamp,
            satelliteCount = satelliteCount,
            gpsFixed = gpsFixed
        )
    }

    private fun readUnsignedInt(
        data: ByteArray,
        offset: Int
    ): Long {
        require(offset >= 0 && offset + 4 <= data.size) {
            "Not enough bytes to read PT20 coordinate"
        }

        return ((data[offset].toLong() and 0xFF) shl 24) or
                ((data[offset + 1].toLong() and 0xFF) shl 16) or
                ((data[offset + 2].toLong() and 0xFF) shl 8) or
                (data[offset + 3].toLong() and 0xFF)
    }
}
