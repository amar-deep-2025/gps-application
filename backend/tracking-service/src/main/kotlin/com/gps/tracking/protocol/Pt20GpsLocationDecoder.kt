package com.gps.tracking.protocol

import com.gps.tracking.protocol.dto.Pt20GpsLocation
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset

class Pt20GpsLocationDecoder {

    fun decode(packet: Pt20Packet): Pt20GpsLocation {
        require(packet.protocolNumber==0x22){
            "Unsupported PT20 protocol: ${packet.protocolNumber}"
        }
        require(packet.content.isNotEmpty()){
            "Pt20 GPS payload cannot be empty"
        }
        require(packet.content.size>=6){
            "PT20 GPS payload must contain at least 6 bytes for timestamp"
        }
        val content = packet.content
        require(content.size>=7){
            "Pt20 GPS payload must contain information byte"
        }
        val satelliteCount=content[6].toInt() and 0x0F

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
        ){
            "Invalid PT20 GPS timestamp"
        }
        val actualYear=2000+year
        val gpsTimestamp=try{ LocalDateTime.of(
            actualYear,
            month,
            day,
            hour,
            minute,
            second
        ).toInstant(ZoneOffset.UTC)
        }catch(ex: DateTimeException){
            throw IllegalArgumentException("Invalid Pt20 GPS timestamp", ex)
        }


        require(content.size>=18){
            "PT20 GPS payload must contain latitude and longitude"
        }

        val rawLatitude=
            ((content[7].toLong() and 0xFF) shl 24) or
                    ((content[8].toLong() and 0xFF) shl 16) or
                    ((content[9].toLong() and 0xFF) shl 8) or
                    (content[10].toLong() and 0xFF)

        val rawLongitude=
            ((content[11].toLong() and 0xFF) shl 24) or
                    ((content[12].toLong() and 0xFF) shl 16) or
                    ((content[13].toLong() and 0xFF) shl 8) or
                    (content[14].toLong() and 0xFF)

        val latitude=rawLatitude/1800000.0
        val longitude=rawLongitude/1800000.0
        val courseState=
            ((content[16].toInt() and 0xFF) shl 8) or
                    (content[17].toInt() and 0xFF)

        val headingState=content[16].toInt() and 0xFF

        val speed=content[15].toInt() and 0xFF
        val heading = courseState and 0x03FF

        val longitudeEast=(headingState and 0x08)!=0
        val latitudeNorth=(headingState and 0x04)!=0
        val gpsFixed=(headingState and 0x10)==0

        val finalLatitude=if (latitudeNorth){
            latitude
        }else{
            -latitude
        }
        val finalLongitude=if(longitudeEast){
            longitude
        }else{
            -longitude
        }

        return Pt20GpsLocation(
            latitude=finalLatitude,
            longitude=finalLongitude,
            speed=speed,
            heading=heading,
            gpsTimestamp=gpsTimestamp,
            satelliteCount=satelliteCount,
            gpsFixed=gpsFixed
        )

    }
}