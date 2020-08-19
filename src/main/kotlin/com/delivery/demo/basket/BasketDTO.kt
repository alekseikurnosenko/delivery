package com.delivery.demo.basket

import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.RestaurantDTO
import com.delivery.demo.restaurant.asDTO
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Basket")
data class BasketDTO(
        val restaurant: RestaurantDTO,
        val items: List<BasketItemDTO>,
        val totalAmount: JacksonConfiguration.MoneyView,
        val isAboveMinimumOrder: Boolean
)

@Schema(name = "BasketItem")
data class BasketItemDTO(
        val dish: DishDTO,
        val quantity: Int,
        val totalPrice: JacksonConfiguration.MoneyView
)

fun Basket.asDTO() = BasketDTO(
        restaurant = restaurant.asDTO(),
        items = items.map { it.asDTO() },
        totalAmount = totalAmount.asDTO(),
        isAboveMinimumOrder = isAboveMinimumOrder
)

fun BasketItem.asDTO() = BasketItemDTO(
        dish = dish.asDTO(),
        quantity = quantity,
        totalPrice = totalPrice.asDTO()
)