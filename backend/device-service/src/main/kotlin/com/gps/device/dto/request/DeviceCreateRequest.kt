package com.gps.device.dto.request


import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class DeviceCreateRequest(

    @field:NotBlank(message = "Serial number is required")
    val serialNumber: String,

    @field:NotBlank(message = "IMEI is required")
    val imei: String,

    @field:NotBlank(message = "Device name is required")
    val deviceName: String,

    @field:NotNull(message = "Device model is required")
    val deviceModelId: UUID,

    val firmwareVersion: String? = null
)