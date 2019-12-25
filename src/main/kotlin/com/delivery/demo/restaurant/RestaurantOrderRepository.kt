package com.delivery.demo.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RestaurantOrderRepository : JpaRepository<RestaurantOrder, UUID> {
    fun findByRestaurantId(restaurantId: UUID): List<RestaurantOrder>
    fun findByRestaurantIdAndStatus(restaurantId: UUID, status: RestaurantOrderStatus): List<RestaurantOrder>
    fun findByRestaurantIdAndOrderId(restaurantId: UUID, orderId: UUID): Optional<RestaurantOrder>
}