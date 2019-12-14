package com.delivery.demo.basket

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.model.Dish
import com.delivery.restaurant.model.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.money.Money
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@RestController
@CrossOrigin
@RequestMapping(
    "/api/basket",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "basket", description = "Current basket")
class BasketController(
    val restaurantRepository: RestaurantRepository,
    val basketRepository: BasketRepository
) {

    @GetMapping("/")
    fun basket(): Basket {
        // if we have user profile - it's easy
        // what if user is a guest?
        return null!!
    }

    @PostMapping("/addItem")
    fun addItemToBasket(input: AddToBasketInput): Basket {
        val restaurant = restaurantRepository.findById(input.restaurantId)
            .orElseThrow { Exception("Restaurant not found") }

        val dish = restaurant.dishes.find { it.id == input.dishId } ?: throw Exception("Dish not found")

        if (input.quantity <= 0) {
            throw Exception("Invalid quantity")
        }

        // find current basket
        // if missing, create one
        // Should it be the controller's job to do it?
        val basket = basket()

        // If basket is not empty
        // And basket already contains dishes from other restaurn
        // throw an error, unless forceNew is passed
        if (basket.items.isNotEmpty() && basket.restaurant.id != input.restaurantId && !input.forceNewBasket) {
            throw Exception("Basket not empty")
        }

        basket.addItem(dish, input.quantity)

        return basket
    }

    @PostMapping("/removeItem")
    fun removeItemFromBasket(input: RemoveFromBasketInput): Basket {
        val restaurant = restaurantRepository.findById(input.restaurantId)
            .orElseThrow { Exception("Restaurant not found") }

        val dish = restaurant.dishes.find { it.id == input.dishId } ?: throw Exception("Dish not found")

        if (input.quantity <= 0) {
            throw Exception("Invalid quantity")
        }

        val basket = basket()

        basket.removeItem(dish, input.quantity)

        return basket
    }
}

data class AddToBasketInput(
    val dishId: UUID,
    val restaurantId: UUID,
    val quantity: Int,
    val forceNewBasket: Boolean = false
)

data class RemoveFromBasketInput(
    val dishId: UUID,
    val restaurantId: UUID,
    val quantity: Int
)