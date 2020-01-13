package com.delivery.demo.courier

import org.springframework.stereotype.Repository
import java.util.*
import kotlin.collections.Map.Entry

@Repository
class CourierLocationRepository {

    private val locationMap = mutableMapOf<UUID, LocationReport>()

    fun updateLocation(courierId: UUID, location: LocationReport) {
        locationMap[courierId] = location
    }

    fun getLocation(courierId: UUID): LocationReport? {
        return locationMap[courierId]
    }

    fun getAll(): Set<Entry<UUID, LocationReport>> = locationMap.entries
}