package com.gps.device.controller

import com.gps.device.dto.request.DeviceCredentialCreateRequest
import com.gps.device.dto.response.DeviceCredentialCreateResponse
import com.gps.device.service.DeviceCredentialService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/device-credentials")
class DeviceCredentialController(
    private val deviceCredentialService: DeviceCredentialService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCredential(
        @Valid @RequestBody request: DeviceCredentialCreateRequest
    ): DeviceCredentialCreateResponse =
        deviceCredentialService.create(request)
}