package com.gps.device.mapper

import com.gps.device.device.entity.DeviceCredential
import com.gps.device.dto.response.DeviceCredentialResponse

object DeviceCredentialMapper {

    fun toResponse(entity: DeviceCredential): DeviceCredentialResponse =
        DeviceCredentialResponse(
            publicId = entity.publicId,
            deviceId = entity.device.publicId,
            credentialType = entity.credentialType,
            status = entity.status,
            expiresAt = entity.expiresAt,
            issuedAt = entity.issuedAt
        )
}