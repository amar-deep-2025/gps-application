package com.gps.tracking.service

import com.gps.tracking.dto.request.LocationRequest
import com.gps.tracking.entity.Location
import com.gps.tracking.repository.LocationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class LocationService(
    private val locationRepository: LocationRepository
) {

    @Transactional
    fun saveLocation(request: LocationRequest): Location {

        require(request.latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90"
        }

        require(request.longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180"
        }

        require(
            request.speed == null ||
                    (request.speed.isFinite() && request.speed >= 0.0)
        ) {
            "Speed must be a non-negative finite number"
        }

        val location = Location(
            devicePublicId = request.devicePublicId,
            latitude = request.latitude,
            longitude = request.longitude,
            speed = request.speed,
            gpsTimestamp = request.gpsTimestamp
        )

        return locationRepository.save(location)
    }
}