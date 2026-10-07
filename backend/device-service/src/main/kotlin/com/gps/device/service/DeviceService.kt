package com.gps.device.service

import com.gps.device.dto.request.DeviceCreateRequest
import com.gps.device.dto.response.DeviceResponse
import com.gps.device.mapper.DeviceMapper
import com.gps.device.repository.DeviceModelRepository
import com.gps.device.repository.DeviceRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DeviceService(
    private val deviceModelRepository: DeviceModelRepository,
    private val deviceRepository: DeviceRepository
) {

    fun create(request: DeviceCreateRequest): DeviceResponse{

        if (deviceRepository.existsByImei(request.imei)){
            throw IllegalArgumentException("IMEI already exists")
        }

        if (deviceRepository.existsBySerialNumber(request.serialNumber)){
            throw IllegalArgumentException("Serial number already exists")
        }

        val deviceModel=deviceModelRepository.findByPublicId(request.deviceModelId)
            ?:throw NoSuchElementException("Device model not found")

        val device=DeviceMapper.toEntity(
            request=request,
            deviceModel=deviceModel
        )
        return DeviceMapper.toResponse(deviceRepository.save(device))
    }

    fun getDeviceByPublicId(publicId:UUID):DeviceResponse{
        val device=deviceRepository.findByPublicId(publicId)
            ?:throw NoSuchElementException("Device not found")
        return DeviceMapper.toResponse(device);
    }

}