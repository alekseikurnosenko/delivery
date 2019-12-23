package com.delivery.demo.order

import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.asDTO

fun Order.asDTO() = OrderDTO(
    id = id.toString(),
    status = status,
    items = items.map { it.asDTO() }
)

fun OrderItem.asDTO() = OrderItemDTO(
    dish = dish.asDTO(),
    quantity = quantity
)

data class OrderDTO(
    val id: String,
    var status: OrderStatus,
    val items: List<OrderItemDTO>
)

data class OrderItemDTO(
    val dish: DishDTO,
    val quantity: Int
)