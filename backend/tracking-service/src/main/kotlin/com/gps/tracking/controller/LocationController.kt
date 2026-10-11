package com.gps.tracking.controller


import com.gps.tracking.dto.LocationResponse
import com.gps.tracking.dto.request.LocationRequest
import com.gps.tracking.mapper.LocationMapper
import com.gps.tracking.service.LocationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val locationService: LocationService,
    private val locationMapper: LocationMapper
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun saveLocation(
        @RequestBody request: LocationRequest
    ): LocationResponse {

        val location = locationService.saveLocation(request)

        return locationMapper.toResponse(location)
    }

    @GetMapping("/{devicePublicId}/latest")
    @ResponseStatus(HttpStatus.OK)
    fun getLatestLocation(@PathVariable devicePublicId:UUID):LocationResponse{
        val location= locationService.getLatestLocation(devicePublicId)
        return locationMapper.toResponse(location)
    }
}