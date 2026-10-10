package com.gps.tracking.mapper

import com.gps.tracking.dto.LocationResponse
import com.gps.tracking.entity.Location
import org.springframework.stereotype.Component

@Component
class LocationMapper {

    fun toResponse(location: Location): LocationResponse {
        return LocationResponse(
            id = location.id,
            devicePublicId = location.devicePublicId,
            latitude = location.latitude,
            longitude = location.longitude,
            speed = location.speed,
            gpsTimestamp = location.gpsTimestamp,
            receivedAt = location.receivedAt
        )
    }
}