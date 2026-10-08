package com.gps.device.device.repository

import com.gps.device.device.entity.DeviceAssignment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceAssignmentRepository : JpaRepository<DeviceAssignment, Long> {

    fun findByPublicId(publicId: UUID): DeviceAssignment?

    fun findByDeviceIdAndUnassignedAtIsNull(
        deviceId: UUID
    ): DeviceAssignment?

    fun existsByDeviceIdAndUnassignedAtIsNull(
        deviceId: UUID
    ): Boolean
}