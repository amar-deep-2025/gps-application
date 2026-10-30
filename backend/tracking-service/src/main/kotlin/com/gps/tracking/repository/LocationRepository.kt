package com.gps.tracking.repository

import com.gps.tracking.entity.Location
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import java.time.Instant

interface LocationRepository : JpaRepository<Location, Long> {

    fun findFirstByDevicePublicIdOrderByGpsTimestampDesc(
        devicePublicId: UUID
    ): Location?

    fun findAllByDevicePublicIdOrderByGpsTimestampDesc(
        devicePublicId: UUID
    ): List<Location>

    fun findAllByDevicePublicIdAndGpsTimestampGreaterThanEqualAndGpsTimestampLessThanOrderByGpsTimestampDesc(
        devicePublicId: UUID,
        from: Instant,
        until: Instant
    ): List<Location>
}
