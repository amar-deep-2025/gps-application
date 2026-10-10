package com.gps.device.controller

import com.gps.device.dto.request.DeviceCreateRequest
import com.gps.device.dto.response.DeviceResponse
import com.gps.device.service.DeviceService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/devices")
class DeviceController(
    private val deviceService: DeviceService
) {
    private val logger =
        LoggerFactory.getLogger(DeviceController::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createDevice(
        @Valid @RequestBody request: DeviceCreateRequest,
        authentication: Authentication?
    ): DeviceResponse {
        logger.info("Started createDevice")

        logger.info("Authentication: {}", authentication)

        val publicId = authentication?.name
            ?: throw IllegalStateException(
                "Authentication is null in DeviceController"
            )

        logger.info("User public ID: {}", publicId)

        return deviceService.create(
            request,
            UUID.fromString(publicId)
        )
    }

    @GetMapping("/{publicId}")
    @ResponseStatus(HttpStatus.OK)
    fun getDeviceByPublicId(
        @PathVariable publicId: UUID
    ): DeviceResponse =
        deviceService.getDeviceByPublicId(publicId)

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllDevices(): List<DeviceResponse> =
        deviceService.getAll()
}