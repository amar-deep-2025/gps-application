package com.gps.tracking.protocol.dto

import java.time.Instant

data class Pt20GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val speed: Int,
    val heading: Int,
    val gpsTimestamp: Instant,
    val satelliteCount: Int,
    val gpsFixed: Boolean
)