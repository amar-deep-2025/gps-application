package com.gps.device.device.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "device_assignments")
class DeviceAssignment(

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

    @Column(name = "device_id", nullable = false)
    var deviceId: UUID,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "assigned_at", nullable = false, updatable = false)
    var assignedAt: Instant = Instant.now(),

    @Column(name = "unassigned_at")
    var unassignedAt: Instant? = null
)