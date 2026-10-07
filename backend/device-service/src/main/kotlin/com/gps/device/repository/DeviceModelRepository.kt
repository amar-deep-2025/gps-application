package com.gps.device.repository

import com.gps.device.device.entity.DeviceModel
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DeviceModelRepository: JpaRepository<DeviceModel, Long>{

    fun findByPublicId(publicId: UUID):DeviceModel?
    fun existByModelNumber(modelNumber: String): Boolean
}