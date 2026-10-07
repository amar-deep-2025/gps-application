package com.gps.device.device.entity

import com.gps.device.enums.DeviceProtocol
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_models")
class DeviceModel(

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

    @Column(nullable = false)
    var manufacturer: String,

    @Column(name = "model_name", nullable = false)
    var modelName: String,

    @Column(name = "model_number")
    var modelNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var protocol: DeviceProtocol,

    @Column(columnDefinition = "TEXT")
    var capabilities: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)