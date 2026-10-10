package com.gps.tracking.protocol

import com.gps.tracking.protocol.dto.Pt20GpsLocation

class Pt20GpsLocationDecoder {

    fun decode(packet: Pt20Packet): Pt20GpsLocation {
        require(packet.protocolNumber==0x22){
            "Unsupported PT20 protocol: ${packet.protocolNumber}"
        }
        require(packet.content.isNotEmpty()){
            "Pt20 GPS payload cannot be empty"
        }
        TODO("GPS payload decoding")
    }
}