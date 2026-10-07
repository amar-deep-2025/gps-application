package com.gps.device.entity

import com.gps.device.device.entity.DeviceModel
import com.gps.device.enums.DeviceLifecycleState
import com.gps.device.enums.DeviceStatus
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "devices",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_device_imei", columnNames = ["imei"]),
        UniqueConstraint(name = "uk_device_serial_number", columnNames = ["serial_number"])
    ]
)
class Device(

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

    @Column(name = "serial_number", nullable = false)
    var serialNumber: String,

    @Column(nullable = false)
    var imei: String,

    @Column(name = "device_name", nullable = false)
    var deviceName: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_model_id", nullable = false)
    var deviceModel: DeviceModel,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: DeviceStatus = DeviceStatus.INACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false)
    var lifecycleState: DeviceLifecycleState = DeviceLifecycleState.REGISTERED,

    @Column(name = "firmware_version")
    var firmwareVersion: String? = null,

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant? = null,

    @Column(name = "registered_at", nullable = false, updatable = false)
    var registeredAt: Instant = Instant.now(),

    @Column(name = "activated_at")
    var activatedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)