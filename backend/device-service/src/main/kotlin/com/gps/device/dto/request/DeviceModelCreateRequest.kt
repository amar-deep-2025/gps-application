package com.gps.device.dto.request

import com.gps.device.enums.DeviceProtocol
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class DeviceModelCreateRequest(

    @field:NotBlank(message = "Manufacturer is required")
    val manufacturer: String,

    @field:NotBlank(message = "Model name is required")
    val modelName: String,

    val modelNumber: String? = null,

    @field:NotNull(message = "Protocol is required")
    val protocol: DeviceProtocol,

    val capabilities: String? = null
)
