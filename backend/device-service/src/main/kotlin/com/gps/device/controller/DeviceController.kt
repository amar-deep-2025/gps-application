package com.gps.device.controller

import com.gps.device.dto.request.DeviceCreateRequest
import com.gps.device.dto.response.DeviceResponse
import com.gps.device.service.DeviceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/devices")
class DeviceController(
    private val deviceService: DeviceService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createDevice(@Valid @RequestBody
                     request:DeviceCreateRequest):DeviceResponse=
        deviceService.create(request)

}