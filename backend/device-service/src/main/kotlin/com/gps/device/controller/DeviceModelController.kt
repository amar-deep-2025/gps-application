package com.gps.device.controller

import com.gps.device.dto.request.DeviceModelCreateRequest
import com.gps.device.dto.response.DeviceModelResponse
import com.gps.device.service.DeviceModelService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/device-models")
class DeviceModelController(
    private val deviceModelService: DeviceModelService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request:DeviceModelCreateRequest
    ):DeviceModelResponse = deviceModelService.create(request)

    @GetMapping("/{publicId}")
    @ResponseStatus(HttpStatus.OK)
    fun getDeviceModelByPublicId(@PathVariable publicId: UUID
    ):DeviceModelResponse=deviceModelService.getByPublicId(publicId);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getAllDevice():List<DeviceModelResponse> = deviceModelService.getAll()
}