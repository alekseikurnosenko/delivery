package com.delivery.demo.delivery

data class DeliveryRequestDTO(
    val orderId: String
)

fun DeliveryRequest.asDTO() = DeliveryRequestDTO(
    orderId = delivery.order.id.toString()
)