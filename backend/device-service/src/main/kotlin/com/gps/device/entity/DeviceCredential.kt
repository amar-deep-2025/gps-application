package com.gps.device.device.entity

import com.gps.device.enums.CredentialStatus

import com.gps.device.entity.Device
import com.gps.device.enums.CredentialType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_credentials")
class DeviceCredential(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(
        name = "public_id",
        nullable = false,
        unique = true,
        updatable = false
    )
    var publicId: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    var device: Device,

    @Enumerated(EnumType.STRING)
    @Column(name = "credential_type", nullable = false)
    var credentialType: CredentialType,

    @Column(name = "credential_hash", nullable = false)
    var credentialHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CredentialStatus = CredentialStatus.ACTIVE,

    @Column(name = "issued_at", nullable = false, updatable = false)
    var issuedAt: Instant = Instant.now(),

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null
)