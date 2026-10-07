package com.gps.device.dto.response

import com.gps.device.enums.DeviceProtocol
import java.time.Instant
import java.util.*

data class DeviceModelResponse(
    val publicId: UUID,
    val manufacturer: String,
    val modelName: String,
    val modelNumber: String?,
    val protocol: DeviceProtocol,
    val capabilities: String?,
    val createdAt: Instant
)