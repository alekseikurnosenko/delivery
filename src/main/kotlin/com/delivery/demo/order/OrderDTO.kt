package com.delivery.demo.order

import com.delivery.demo.Address
import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.courier.CourierDTO
import com.delivery.demo.courier.asDTO
import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.RestaurantDTO
import com.delivery.demo.restaurant.asDTO
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

fun Order.asDTO() = OrderDTO(
        id = id.toString(),
        createdAt = (createdAt ?: Date()).toInstant().toString(), // FIXME: Should be part of domain
        totalAmount = totalAmount.asDTO(),
        deliveryAddress = deliveryAddress,
        status = status,
        items = items.map { it.asDTO() },
        restaurant = restaurant.asDTO(),
        courier = delivery.assignedCourier?.asDTO()
)

fun OrderItem.asDTO() = OrderItemDTO(
        dish = dish.asDTO(),
        quantity = quantity
)

@Schema(name = "Order")
data class OrderDTO(
        val id: String,
        val createdAt: String,
        val totalAmount: JacksonConfiguration.MoneyView,
        val deliveryAddress: Address,
        val restaurant: RestaurantDTO,
        val courier: CourierDTO?,
        var status: OrderStatus,
        val items: List<OrderItemDTO>
)

data class OrderItemDTO(
        val dish: DishDTO,
        val quantity: Int
)