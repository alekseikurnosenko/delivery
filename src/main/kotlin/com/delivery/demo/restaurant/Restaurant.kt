package com.delivery.demo.restaurant

import com.delivery.demo.Address
import com.delivery.demo.basket.Basket
import com.delivery.demo.order.Order
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id val id: UUID,
    val name: String,
    val minimumOrderAmount: Money? = null,
    @Embedded val address: Address,
    val currency: CurrencyUnit
) {

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
            id = UUID.randomUUID(),
            owner = owner,
            deliveryAddress = deliveryAddress,
            restaurant = this
        )
    }

    fun placeOrder(order: Order): RestaurantOrder {
        if (!isAcceptingOrders) {
            throw Exception("Cannot place order since the restaurant is not accepting any")
        }
        return RestaurantOrder(
            restaurantId = id,
            order = order
        )
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