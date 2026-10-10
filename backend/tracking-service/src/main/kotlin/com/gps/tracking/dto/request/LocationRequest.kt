package com.gps.tracking.dto.request

import java.time.Instant
import java.util.UUID

data class LocationRequest(
    val devicePublicId: UUID,
    val latitude: Double,
    val longitude: Double,
    val speed: Double? = null,
    val gpsTimestamp: Instant
)