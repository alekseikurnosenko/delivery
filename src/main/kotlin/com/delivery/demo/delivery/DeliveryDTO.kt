package com.delivery.demo.delivery

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Delivery")
data class DeliveryDTO(
    val id: String
)

fun Delivery.asDTO() = DeliveryDTO(
    id = id.toString()
)