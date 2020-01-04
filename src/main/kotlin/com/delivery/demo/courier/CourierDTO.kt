package com.delivery.demo.courier

import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderDTO
import com.delivery.demo.order.asDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Courier")
data class CourierDTO(
    val id: String,
    val fullName: String,
    val onShift: Boolean,
    val location: LatLng?,
    val activeOrders: List<OrderDTO>?
)

fun Courier.asDTO(withOrders: Boolean = false) = CourierDTO(
    id = id.toString(),
    fullName = fullName,
    onShift = onShift,
    location = location?.latLng,
    activeOrders = if (withOrders) activeOrders.map<Order, OrderDTO> { it.asDTO() } else null
)