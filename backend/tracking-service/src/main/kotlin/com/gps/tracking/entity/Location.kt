package com.gps.tracking.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "device_locations",
    indexes = [
        Index(
            name = "idx_location_device_time",
            columnList = "device_public_id, gps_timestamp"
        )
    ]
)
class Location(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "device_public_id", nullable = false)
    val devicePublicId: UUID,

    @Column(nullable = false)
    val latitude: Double,

    @Column(nullable = false)
    val longitude: Double,

    val speed: Double? = null,

    @Column(name = "gps_timestamp", nullable = false)
    val gpsTimestamp: Instant,

    @Column(name = "received_at", nullable = false, updatable = false)
    val receivedAt: Instant = Instant.now()
)