package com.gps.device.dto.response


import com.gps.device.enums.DeviceLifecycleState
import com.gps.device.enums.DeviceStatus
import java.time.Instant
import java.util.UUID

data class DeviceResponse(
    val publicId: UUID,
    val serialNumber: String,
    val imei: String,
    val deviceName: String,
    val deviceModelId: UUID,
    val status: DeviceStatus,
    val lifecycleState: DeviceLifecycleState,
    val firmwareVersion: String?,
    val lastSeenAt: Instant?,
    val registeredAt: Instant?,
    val activatedAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)