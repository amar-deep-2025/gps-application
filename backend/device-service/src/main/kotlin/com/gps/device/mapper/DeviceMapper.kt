package com.gps.device.mapper
import com.gps.device.dto.response.DeviceResponse
import com.gps.device.entity.Device
import com.gps.device.device.entity.DeviceModel
import com.gps.device.dto.request.DeviceCreateRequest
import com.gps.device.enums.DeviceLifecycleState
import com.gps.device.enums.DeviceStatus
import java.util.*

object DeviceMapper {

    fun toEntity(
        request: DeviceCreateRequest,
        deviceModel: DeviceModel,
        userPublicId: UUID
    ): Device =
        Device(
            serialNumber = request.serialNumber,
            imei = request.imei,
            deviceName = request.deviceName,
            deviceModel = deviceModel,
            status = DeviceStatus.INACTIVE,
            lifecycleState = DeviceLifecycleState.REGISTERED,
            firmwareVersion = request.firmwareVersion,
            userPublicId = userPublicId

        )

    fun toResponse(entity: Device): DeviceResponse =
        DeviceResponse(
            publicId = entity.publicId,
            serialNumber = entity.serialNumber,
            imei = entity.imei,
            deviceName = entity.deviceName,
            deviceModelId = entity.deviceModel.publicId,
            status = entity.status,
            lifecycleState = entity.lifecycleState,
            firmwareVersion = entity.firmwareVersion,
            lastSeenAt = entity.lastSeenAt,
            registeredAt = entity.registeredAt,
            activatedAt = entity.activatedAt,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
}