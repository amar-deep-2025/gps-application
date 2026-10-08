package com.gps.device.service

import com.gps.device.device.entity.DeviceAssignment
import com.gps.device.device.repository.DeviceAssignmentRepository
import com.gps.device.repository.DeviceRepository
import com.gps.device.dto.request.DeviceAssignmentCreateRequest
import com.gps.device.dto.response.DeviceAssignmentResponse
import com.gps.device.mapper.DeviceAssignmentMapper
import org.springframework.stereotype.Service

@Service
class DeviceAssignmentService(
    private val deviceAssignmentRepository: DeviceAssignmentRepository,
    private val deviceRepository: DeviceRepository
) {

    fun create(
        request: DeviceAssignmentCreateRequest
    ): DeviceAssignmentResponse {

        deviceRepository.findByPublicId(request.deviceId)
            ?: throw NoSuchElementException("Device not found")

        if (deviceAssignmentRepository
                .existsByDeviceIdAndUnassignedAtIsNull(request.deviceId)
        ) {
            throw IllegalArgumentException(
                "Device is already assigned"
            )
        }

        val assignment = DeviceAssignment(
            deviceId = request.deviceId,
            userId = request.userId
        )

        return DeviceAssignmentMapper.toResponse(
            deviceAssignmentRepository.save(assignment)
        )
    }
}