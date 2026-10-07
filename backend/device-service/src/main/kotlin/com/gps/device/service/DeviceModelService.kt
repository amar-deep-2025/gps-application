package com.gps.device.service

import com.gps.device.device.mapper.DeviceModelMapper
import com.gps.device.dto.request.DeviceModelCreateRequest
import com.gps.device.dto.response.DeviceModelResponse
import com.gps.device.repository.DeviceModelRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeviceModelService(
    private val deviceModelRepository: DeviceModelRepository
) {

    fun create(request:DeviceModelCreateRequest):DeviceModelResponse{

        if (request.modelNumber!=null &&
            deviceModelRepository.existsByModelNumber(request.modelNumber)){

            throw IllegalArgumentException("Model number already exists")

        }

        val deviceModel=DeviceModelMapper.toEntity(request);
        return DeviceModelMapper.toResponse(
            deviceModelRepository.save(deviceModel)
        )
    }
    fun  getByPublicId(publicId:UUID):DeviceModelResponse{

        val deviceModel=deviceModelRepository.findByPublicId(publicId)
            ?:throw NoSuchElementException("Device model not found")
        return DeviceModelMapper.toResponse(deviceModel)
    }

    fun getAll():List<DeviceModelResponse> =
        deviceModelRepository.findAll()
            .map { deviceModel ->
                DeviceModelMapper.toResponse(deviceModel)
            }

}