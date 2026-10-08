package com.gps.device.dto.request

import com.gps.device.enums.CredentialType
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class DeviceCredentialCreateRequest(

    @field:NotNull(message = "Device ID is required")
    val deviceId: UUID,

    @field:NotNull(message = "Credential type is required")
    val credentialType: CredentialType,

    val expiresAt: Instant? = null
)