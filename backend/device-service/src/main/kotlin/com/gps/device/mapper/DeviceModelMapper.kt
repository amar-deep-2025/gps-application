package com.gps.device.device.mapper


import com.gps.device.device.entity.DeviceModel
import com.gps.device.dto.request.DeviceModelCreateRequest
import com.gps.device.dto.response.DeviceModelResponse

object DeviceModelMapper {

    fun toEntity(request: DeviceModelCreateRequest): DeviceModel =
        DeviceModel(
            manufacturer = request.manufacturer,
            modelName = request.modelName,
            modelNumber = request.modelNumber,
            protocol = request.protocol,
            capabilities = request.capabilities
        )

    fun toResponse(entity: DeviceModel): DeviceModelResponse =
        DeviceModelResponse(
            publicId = entity.publicId,
            manufacturer = entity.manufacturer,
            modelName = entity.modelName,
            modelNumber = entity.modelNumber,
            protocol = entity.protocol,
            capabilities = entity.capabilities,
            createdAt = entity.createdAt
        )
}