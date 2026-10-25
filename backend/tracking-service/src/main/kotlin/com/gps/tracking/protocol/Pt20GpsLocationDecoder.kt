package com.gps.tracking.protocol

import com.gps.tracking.protocol.dto.Pt20GpsLocation
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Duration

class Pt20GpsLocationDecoder {

    fun decode(packet: Pt20Packet): Pt20GpsLocation {
        require(packet.protocolNumber == 0x22) {
            "Unsupported PT20 protocol: ${packet.protocolNumber}"
        }

        val content = packet.content

        require(content.size >= 18) {
            "PT20 GPS payload must contain at least 18 bytes. Received size: ${content.size}"
        }

        // 1. Parse Temporal GPS Structural Coordinates
        val year = content[0].toInt() and 0xFF
        val month = content[1].toInt() and 0xFF
        val day = content[2].toInt() and 0xFF
        val hour = content[3].toInt() and 0xFF
        val minute = content[4].toInt() and 0xFF
        val second = content[5].toInt() and 0xFF

        require(
            year in 0..99 &&
                    month in 1..12 &&
                    day in 1..31 &&
                    hour in 0..23 &&
                    minute in 0..59 &&
                    second in 0..59
        ) {
            "Invalid PT20 GPS timestamp parsed from hardware payload stream"
        }

        val rawLocalTime = try {
            LocalDateTime.of(2000 + year, month, day, hour, minute, second).toInstant(ZoneOffset.UTC)
        } catch (ex: DateTimeException) {
            throw IllegalArgumentException("Invalid Pt20 GPS timestamp transformation boundary", ex)
        }

        var gpsTimestamp=rawLocalTime.atZone(ZoneOffset.UTC).toInstant()
        if ((2000+year) < 2015){
            gpsTimestamp=gpsTimestamp.plus(Duration.ofDays(7168))
        }
        // 2. Parse Satellite Density Metadata
        val satelliteCount = content[6].toInt() and 0x0F

        // 3. Parse Raw Positional Coordinates (Avoid implicit math sign extensions)
        val rawLatitude = ((content[7].toLong() and 0xFF) shl 24) or
                ((content[8].toLong() and 0xFF) shl 16) or
                ((content[9].toLong() and 0xFF) shl 8) or
                (content[10].toLong() and 0xFF)

        val rawLongitude = ((content[11].toLong() and 0xFF) shl 24) or
                ((content[12].toLong() and 0xFF) shl 16) or
                ((content[13].toLong() and 0xFF) shl 8) or
                (content[14].toLong() and 0xFF)

        val latitudeDecimal = rawLatitude / 1800000.0
        val longitudeDecimal = rawLongitude / 1800000.0

        // 4. Parse Operational Metrics (Speed and 16-bit Course/Status Field)
        val speed = content[15].toInt() and 0xFF

        val courseState = ((content[16].toInt() and 0xFF) shl 8) or
                (content[17].toInt() and 0xFF)

        // Standard PT20/GT06 bitwise mappings inside the 2-byte structural course field:
        val heading = courseState and 0x03FF
        val gpsFixed = (courseState and 0x1000) != 0
        val longitudeEast = (courseState and 0x0800) != 0
        val latitudeNorth = (courseState and 0x0400) != 0

        // Apply directional hemispherics signs properly based on status flags
        val finalLatitude = if (latitudeNorth) latitudeDecimal else -latitudeDecimal
        val finalLongitude = if (longitudeEast) longitudeDecimal else -longitudeDecimal

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
}
