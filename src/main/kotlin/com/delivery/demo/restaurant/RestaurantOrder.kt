package com.delivery.demo.restaurant

import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurant_orders")
class RestaurantOrder(
    @Id val restaurantId: UUID,
    status: RestaurantOrderStatus,
    @OneToOne @JoinColumn(name = "order_id") val order: Order
) {
    var status: RestaurantOrderStatus = status
        protected set

    fun startPreparing() {
        if (status == RestaurantOrderStatus.Completed) {
            throw Exception("Cannot start preparing completed orders")
        }
        status = RestaurantOrderStatus.Active
        // 1. How do we update Order status now?
        // Definitely not from here
        // Via event?
    }

    fun finishPreparing() {
        status = RestaurantOrderStatus.Completed
    }
}

enum class RestaurantOrderStatus {
    Pending,
    Active,
    Completed
}