package com.gps.device.service

import com.gps.device.device.entity.DeviceCredential
import com.gps.device.enums.CredentialStatus
import com.gps.device.device.repository.DeviceCredentialRepository
import com.gps.device.repository.DeviceRepository
import com.gps.device.dto.request.DeviceCredentialCreateRequest
import com.gps.device.dto.response.DeviceCredentialCreateResponse
import com.gps.device.mapper.DeviceCredentialMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class DeviceCredentialService(
    private val deviceCredentialRepository: DeviceCredentialRepository,
    private val deviceRepository: DeviceRepository,

    @Value("\${device.credential.expiry-days}")
    private val credentialExpiryDays: Long
) {

    fun create(
        request: DeviceCredentialCreateRequest
    ): DeviceCredentialCreateResponse {

        val device = deviceRepository.findByPublicId(request.deviceId)
            ?: throw NoSuchElementException("Device not found")

        val now=Instant.now()

        val expiresAt= request.expiresAt
            ?: now.plus(credentialExpiryDays, ChronoUnit.DAYS)

        if (!expiresAt.isAfter(now)){
            throw IllegalArgumentException("Credentials expiry must be in the future")
        }

        val rawCredential = generateCredential()

        val credentialHash = hashCredential(rawCredential)


        val credential = DeviceCredential(
            device = device,
            credentialType = request.credentialType,
            credentialHash = credentialHash,
            status = CredentialStatus.ACTIVE,
            expiresAt = expiresAt
        )

        val savedCredential =
            deviceCredentialRepository.save(credential)

        return DeviceCredentialCreateResponse(
            publicId = savedCredential.publicId,
            deviceId = savedCredential.device.publicId,
            credentialType = savedCredential.credentialType,
            credential = rawCredential,
            status = savedCredential.status,
            expiresAt = savedCredential.expiresAt,
            issuedAt = savedCredential.issuedAt
        )
    }

    private fun generateCredential(): String {

        val bytes = ByteArray(32)

        SecureRandom().nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }

    private fun hashCredential(value: String): String {

        val digest = MessageDigest.getInstance("SHA-256")

        return digest
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte ->
                "%02x".format(byte)
            }
    }
}