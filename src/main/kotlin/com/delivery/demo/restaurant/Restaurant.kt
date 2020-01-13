package com.delivery.demo.restaurant

import com.delivery.demo.Address
import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.basket.Basket
import com.delivery.demo.basket.BasketItem
import com.delivery.demo.order.Order
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurants")
class Restaurant private constructor(
    val name: String,
    @Embedded val address: Address,
    val minimumOrderAmount: Money? = null,
    val currency: CurrencyUnit,
    val accountId: String
) : Aggregate() {

    @OneToMany(mappedBy = "restaurant", cascade = [CascadeType.ALL])
    var dishes: MutableList<Dish> = mutableListOf()
        protected set

    val isAcceptingOrders: Boolean
        get() = true // TOOD: operating times?

    fun addDish(name: String, price: Money): Dish {
        val dish = Dish(
            id = UUID.randomUUID(),
            name = name,
            price = price,
            restaurant = this
        )
        dishes.add(dish)
        return dish
    }

    fun newBasket(owner: String, deliveryAddress: Address): Basket {
        return Basket(
            owner = owner,
            deliveryAddress = deliveryAddress,
            restaurant = this
        )
    }

    fun placeOrder(
        userId: String,
        deliveryAddress: Address,
        items: List<BasketItem>
    ): Order {
        if (!isAcceptingOrders) {
            throw Exception("Cannot place order since the restaurant is not accepting any")
        }

        return Order.place(
            userId = userId,
            restaurant = this,
            deliveryAddress = deliveryAddress,
            items = items
        )
    }

    companion object {
        fun new(
            accountId: String,
            name: String,
            address: Address,
            currency: CurrencyUnit,
            minimumOrderAmount: Money?
        ): Restaurant {
            val restaurant = Restaurant(
                accountId = accountId,
                name = name,
                address = address,
                currency = currency,
                minimumOrderAmount = minimumOrderAmount
            )

            restaurant.registerEvent(RestaurantAdded(accountId, restaurant.id))

            return restaurant
        }
    }

}

@Entity
@Table(name = "dishes")
class Dish(
    @Id
    val id: UUID,
    val name: String,
    val price: Money,
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    val restaurant: Restaurant
)

data class RestaurantAdded(
    val accountId: String,
    val restaurantId: UUID
) : DomainEvent