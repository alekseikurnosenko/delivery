package com.delivery.demo.courier

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CourierRepository : JpaRepository<Courier, UUID> {
    fun findByIdInAndOnShift(ids: List<UUID>, onShift: Boolean = true): MutableList<Courier>
    fun findByOnShift(onShift: Boolean): MutableList<Courier>
    fun findByAccountId(userId: String): Optional<Courier>

    fun countByOnShift(onShift: Boolean): Long
}