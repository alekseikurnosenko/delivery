package com.delivery.demo.delivery

import com.delivery.demo.Address

data class DeliveryRequestDTO(
        val orderId: String,
        val courierId: String,
        val pickup: Address,
        val dropoff: Address
)

fun DeliveryRequest.asDTO() = DeliveryRequestDTO(
        orderId = delivery.order.id.toString(),
        courierId = courier.id.toString(),
        pickup = delivery.pickup,
        dropoff = delivery.dropoff
)