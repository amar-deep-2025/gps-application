package com.gps.device.repository

import com.gps.device.entity.Device
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import com.gps.device.enums.DeviceStatus
import java.time.Instant

interface DeviceRepository:JpaRepository<Device,Long> {

    fun findByPublicId(publicId:UUID): Device?
    fun findByImei(imei:String): Device?
    fun findAllByStatusAndLastSeenAtBefore(status: DeviceStatus, cutoff: Instant): List<Device>
    fun existsByImei(imei:String):Boolean
    fun existsBySerialNumber(serialNumber:String):Boolean
}
