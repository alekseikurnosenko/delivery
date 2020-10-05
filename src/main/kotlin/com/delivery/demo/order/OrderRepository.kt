package com.delivery.demo.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OrderRepository : JpaRepository<Order, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<Order>
    fun findByStatusAndRestaurantId(status: OrderStatus, restaurantId: UUID): List<Order>
}