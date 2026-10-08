package com.gps.device.dto.response

import java.time.Instant
import java.util.UUID

data class DeviceAssignmentResponse(
    val publicId: UUID,
    val deviceId: UUID,
    val userId: UUID,
    val assignedAt: Instant,
    val unassignedAt: Instant?
)