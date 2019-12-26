package com.delivery.demo.courier

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CourierOrderRepository : JpaRepository<CourierOrder, UUID> {
    fun findByCourierId(courierId: UUID): List<CourierOrder>
    fun findByCourierIdAndOrderId(courierId: UUID, orderId: UUID): Optional<CourierOrder>
}