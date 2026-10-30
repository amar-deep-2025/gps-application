package com.gps.device.service

import com.gps.device.dto.request.DeviceCreateRequest
import com.gps.device.dto.response.DeviceResponse
import com.gps.device.mapper.DeviceMapper
import com.gps.device.enums.DeviceStatus
import com.gps.device.repository.DeviceModelRepository
import com.gps.device.repository.DeviceRepository
import org.springframework.stereotype.Service
import java.util.UUID
import java.time.Instant
import java.time.Duration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceService(
    private val deviceModelRepository: DeviceModelRepository,
    private val deviceRepository: DeviceRepository
) {

    fun create(request: DeviceCreateRequest,userPublicId:UUID): DeviceResponse{

        if (deviceRepository.existsByImei(request.imei)){
            throw IllegalArgumentException("IMEI already exists")
        }

        if (deviceRepository.existsBySerialNumber(request.serialNumber)){
            throw IllegalArgumentException("Serial number already exists")
        }

        val deviceModel=deviceModelRepository.findByPublicId(request.deviceModelId)
            ?:throw NoSuchElementException("Device model not found")

        val device=DeviceMapper.toEntity(
            request = request,
            deviceModel = deviceModel,
            userPublicId = userPublicId
        )
        return DeviceMapper.toResponse(deviceRepository.save(device))
    }

    fun getDeviceByPublicId(publicId:UUID):DeviceResponse{
        val device=deviceRepository.findByPublicId(publicId)
            ?:throw NoSuchElementException("Device not found")
        return DeviceMapper.toResponse(device);
    }

    fun getAll():List<DeviceResponse> =
        deviceRepository.findAll()
            .map { device-> DeviceMapper.toResponse(device)  }


    fun getDeviceByImei(imei:String):DeviceResponse{
        val device=deviceRepository.findByImei(imei)
            ?:throw NoSuchElementException("Device not found")

        return DeviceMapper.toResponse(device)
    }

    @Transactional
    fun markDeviceSeen(publicId: UUID): DeviceResponse {
        val device = deviceRepository.findByPublicId(publicId)
            ?: throw NoSuchElementException("Device not found")
        if (device.status != DeviceStatus.BLOCKED) {
            device.status = DeviceStatus.ACTIVE
        }
        val now = Instant.now()
        device.lastSeenAt = now
        device.updatedAt = now
        return DeviceMapper.toResponse(deviceRepository.save(device))
    }

    @Transactional
    @Scheduled(fixedDelayString = "\${device.status.refresh-interval-ms:30000}")
    fun markStaleDevicesInactive() {
        val cutoff = Instant.now().minus(Duration.ofMinutes(2))
        val staleDevices = deviceRepository.findAllByStatusAndLastSeenAtBefore(
            DeviceStatus.ACTIVE,
            cutoff
        )
        if (staleDevices.isEmpty()) return

        val now = Instant.now()
        staleDevices.forEach {
            it.status = DeviceStatus.INACTIVE
            it.updatedAt = now
        }
        deviceRepository.saveAll(staleDevices)
    }
}
