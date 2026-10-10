package com.gps.tracking.dto

import java.time.Instant
import java.util.UUID

data class LocationResponse(
    val id: Long?,
    val devicePublicId: UUID,
    val latitude: Double,
    val longitude: Double,
    val speed: Double?,
    val gpsTimestamp: Instant,
    val receivedAt: Instant
)