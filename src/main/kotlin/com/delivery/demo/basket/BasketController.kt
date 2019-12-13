package com.delivery.demo.basket

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.model.Dish
import com.delivery.restaurant.model.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
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

interface BasketRepository : JpaRepository<Basket, UUID>

@Entity
data class Basket(
    val id: UUID,
    val restaurant: Restaurant,
    @OneToMany(mappedBy = "", cascade = [CascadeType.ALL])
    private val _items: MutableList<BasketItem> = mutableListOf()
) {

    val items: List<BasketItem>
        get() = _items

    val totalAmount: Money
        get() {
            if (_items.isEmpty()) {
                return Money(0, restaurant.currency)
            } else {
                _items.reduce { acc, basketItem ->
                    acc + basketItem.dish.price * basketItem.quantity
                }
            }
        }

    val isAboveMinmumOrder: Boolean
        get() {
            return restaurant.minimumOrderAmount?.let {
                it < totalAmount
            } ?: false
        }

    fun addItem(dish: Dish, quantity: Int) {
        val basketItemIndex = _items.indexOfFirst { it.dish == dish }
        if (basketItemIndex != -1) {
            val basketItem = _items[basketItemIndex]
            _items[basketItemIndex] = basketItem.copy(quantity = basketItem.quantity + quantity)
        } else {
            _items.add(BasketItem(dish, quantity, this))
        }
    }

    fun removeItem(dish: Dish, quantity: Int) {
        val basketItemIndex = _items.indexOfFirst { it.dish == dish }
        val basketItem = if (basketItemIndex > 0) _items[basketItemIndex] else return
        if (quantity >= basketItem.quantity) {
            _items.removeAt(basketItemIndex)
        } else {
            _items[basketItemIndex] = basketItem.copy(quantity = basketItem.quantity - quantity)
        }
    }

    fun checkout() {
        if (!isAboveMinmumOrder) {
            throw Exception("Cannot checkout: below minimum order")
        }

        // Generate PaymentIntent using `totalAmount`
        // Send it to client?
//        status = WaitingForPayment.

    }
}

@Entity
data class BasketItem(
    val dish: Dish,
    val quantity: Int,
    @OneToOne
    @JoinColumn(name = "basket_id")
    @JsonIgnore
    val basket: Basket
)