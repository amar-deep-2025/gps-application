package com.gps.device.mapper

import com.gps.device.device.entity.DeviceAssignment
import com.gps.device.dto.response.DeviceAssignmentResponse

object DeviceAssignmentMapper {

    fun toResponse(entity: DeviceAssignment): DeviceAssignmentResponse =
        DeviceAssignmentResponse(
            publicId = entity.publicId,
            deviceId = entity.deviceId,
            userId = entity.userId,
            assignedAt = entity.assignedAt,
            unassignedAt = entity.unassignedAt
        )
}