package com.delivery.demo.basket

import com.delivery.demo.Address
import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.courier.LatLng
import com.delivery.demo.order.OrderDTO
import com.delivery.demo.order.asDTO
import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.demo.restaurant.asDTO
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@CrossOrigin
@RequestMapping(
    "/api/basket",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "basket", description = "Current basket")
class BasketController(
    val restaurantRepository: RestaurantRepository,
    val basketRepository: BasketRepository,
    val placeOrderUseCase: PlacerOrderUseCase
) {

    @GetMapping("")
    fun basket(): BasketDTO? {
        // if we have user profile - it's easy
        // what if user is a guest?
        val owner = SecurityContextHolder.getContext().authentication.principal as String

        return basketRepository.findByOwner(owner).map { it.asDTO() }.orElse(null)
    }

    @PostMapping("/addItem", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun addItemToBasket(@RequestBody @Valid input: AddItemToBasketInput): BasketDTO {
        val restaurant = restaurantRepository.findById(input.restaurantId)
            .orElseThrow { Exception("Restaurant not found") }

        val dish = restaurant.dishes.find { it.id == input.dishId } ?: throw Exception("Dish not found")

        if (input.quantity <= 0) {
            throw Exception("Invalid quantity")
        }

        val owner = SecurityContextHolder.getContext().authentication.principal as String
        // find current basket
        // if missing, create one
        // Should it be the controller's job to do it?
        val basket = basketRepository.findByOwner(owner).orElseGet {
            val newBasket = restaurant.newBasket(owner)
            basketRepository.save(newBasket)
        }

        // If basket is not empty
        // And basket already contains dishes from other restaurn
        // throw an error, unless forceNew is passed
        if (basket.items.isNotEmpty() && basket.restaurant.id != input.restaurantId && !input.forceNewBasket) {
            throw Exception("Basket not empty")
        }

        basket.addItem(dish, input.quantity)
        basketRepository.flush()

        return basket.asDTO()
    }

    @PostMapping("/removeItem", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun removeItemFromBasket(@RequestBody @Valid input: RemoveFromBasketInput): Basket {
        val restaurant = restaurantRepository.findById(input.restaurantId)
            .orElseThrow { Exception("Restaurant not found") }

        val dish = restaurant.dishes.find { it.id == input.dishId } ?: throw Exception("Dish not found")

        if (input.quantity <= 0) {
            throw Exception("Invalid quantity")
        }

        val owner = SecurityContextHolder.getContext().authentication.principal as String
        val basket = basketRepository.findByOwner(owner).orElseThrow { Exception("No basket avaialble") }

        basket.removeItem(dish, input.quantity)

        return basket
    }

    @PostMapping("/checkout")
    fun checkout(): OrderDTO {
        val owner = SecurityContextHolder.getContext().authentication.principal as String
        val basket = basketRepository.findByOwner(owner).orElseThrow { Exception("No basket avaialble") }

        // TODO: Address should be set at the very beginning before searching
        return placeOrderUseCase.place(basket, Address(LatLng(-5.0f, -5.0f), "Some", "City", "US")).asDTO()
    }
}

data class AddItemToBasketInput(
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

data class BasketDTO(
    val items: List<BasketItemDTO>,
    val totalAmount: JacksonConfiguration.MoneyView,
    val isAboveMinimumOrder: Boolean
)

data class BasketItemDTO(
    val dish: DishDTO,
    val quantity: Int
)

fun Basket.asDTO() = BasketDTO(
    items = items.map { it.asDTO() },
    totalAmount = totalAmount.asDTO(),
    isAboveMinimumOrder = isAboveMinimumOrder
)

fun BasketItem.asDTO() = BasketItemDTO(
    dish = dish.asDTO(),
    quantity = quantity
)
