package com.delivery.demo.delivery

data class DeliveryRequestDTO(
    val deliveryId: String
)

fun DeliveryRequest.asDTO() = DeliveryRequestDTO(
    deliveryId = delivery.id.toString()
)