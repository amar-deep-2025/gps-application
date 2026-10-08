package com.gps.device.device.repository

import com.gps.device.device.entity.DeviceCredential
import org.springframework.data.jpa.repository.JpaRepository

interface DeviceCredentialRepository : JpaRepository<DeviceCredential, Long> {

    fun findByDeviceId(deviceId: Long): List<DeviceCredential>

    fun findByCredentialHash(credentialHash: String): DeviceCredential?

    fun existsByCredentialHash(credentialHash: String): Boolean
}