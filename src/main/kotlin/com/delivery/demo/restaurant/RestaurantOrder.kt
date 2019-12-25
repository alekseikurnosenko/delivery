package com.delivery.demo.restaurant

import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurant_orders")
data class RestaurantOrder(
    @Id
    val restaurantId: UUID,
    val status: RestaurantOrderStatus,
    @OneToOne
    @JoinColumn(name = "order_id")
    val order: Order
)

enum class RestaurantOrderStatus {
    Pending,
    Active,
    Completed
}