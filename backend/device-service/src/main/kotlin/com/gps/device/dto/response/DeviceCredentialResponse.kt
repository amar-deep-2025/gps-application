package com.gps.device.dto.response

import com.gps.device.enums.CredentialStatus
import com.gps.device.enums.CredentialType

import java.time.Instant
import java.util.UUID

data class DeviceCredentialResponse(
    val publicId: UUID,
    val deviceId: UUID,
    val credentialType: CredentialType,
    val status: CredentialStatus,
    val expiresAt: Instant?,
    val issuedAt: Instant?
)