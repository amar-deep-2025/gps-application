package com.gps.device.dto.request

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class DeviceAssignmentCreateRequest(

    @field:NotNull(message = "Device ID is required")
    val deviceId: UUID,

    @field:NotNull(message = "User ID is required")
    val userId: UUID
)