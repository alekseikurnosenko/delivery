package com.delivery.demo.restaurant

import com.delivery.demo.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RestaurantOrderRepository : JpaRepository<RestaurantOrder, RestaurantOrderId> {
    fun findByRestaurantId(restaurantId: UUID): List<RestaurantOrder>
    fun findByRestaurantIdAndOrderStatus(restaurantId: UUID, status: OrderStatus): List<RestaurantOrder>
    fun findByRestaurantIdAndOrderId(restaurantId: UUID, orderId: UUID): Optional<RestaurantOrder>
}