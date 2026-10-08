package com.gps.device.controller

import com.gps.device.dto.request.DeviceAssignmentCreateRequest
import com.gps.device.dto.response.DeviceAssignmentResponse
import com.gps.device.service.DeviceAssignmentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/device-assignments")
class DeviceAssignmentController(
    private val deviceAssignmentService: DeviceAssignmentService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAssignment(
        @Valid @RequestBody request: DeviceAssignmentCreateRequest
    ): DeviceAssignmentResponse =
        deviceAssignmentService.create(request)
}