package com.delivery.demo.restaurant

import com.delivery.demo.AbstractEntity
import com.delivery.demo.Address
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
        name: String,
        address: Address,
        val currency: CurrencyUnit,
        val accountId: String,
        imageUrl: String? = null
) : AbstractEntity() {

    var name: String = name
        protected set

    @Embedded
    var address: Address = address
        protected set

    var minimumOrderAmount: Money? = null
        protected set

    var imageUrl: String? = imageUrl
        protected set

    @OneToMany(mappedBy = "restaurant", cascade = [CascadeType.ALL])
    var dishes: MutableList<Dish> = mutableListOf()
        protected set

    val isAcceptingOrders: Boolean
        get() = true // TOOD: operating times?

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

    fun delete() {
        isDeleted = true
        dishes.forEach { it.delete() }
        // TODO: Event if needed
    }

    fun update(
            name: String,
            address: Address,
            imageUrl: String?
    ) {
        this.name = name
        this.address = address
        this.imageUrl = imageUrl

        // Generate event if needed?
    }

    fun addDish(name: String, price: Money, imageUrl: String?): Dish {
        val dish = Dish(
                name = name,
                price = price,
                imageUrl = imageUrl,
                restaurant = this
        )
        dishes.add(dish)
        return dish
    }

    fun editDish(dishId: UUID, name: String, price: Money, imageUrl: String?): Dish {
        val dish = dishes.find { it.id == dishId } ?: throw Exception("Unknown dish with id=$dishId")

        // TODO: Encapsulate into Dish entity?
        dish.name = name
        dish.price = price
        dish.imageUrl = imageUrl

        return dish
    }

    fun deleteDish(dishId: UUID) {
        val dish = dishes.find { it.id == dishId } ?: throw Exception("Unknown dish with id=$dishId")

        dish.delete()
    }

    companion object {
        fun new(
                accountId: String,
                name: String,
                address: Address,
                currency: CurrencyUnit,
                imageUrl: String?
        ): Restaurant {
            val restaurant = Restaurant(
                    accountId = accountId,
                    name = name,
                    address = address,
                    currency = currency,
                    imageUrl = imageUrl
            )

            restaurant.registerEvent(RestaurantAdded(accountId, restaurant.id))

            return restaurant
        }
    }

}

@Entity
@Table(name = "dishes")
class Dish(
        var name: String,
        var price: Money,
        var imageUrl: String?,
        @ManyToOne
        @JoinColumn(name = "restaurant_id")
        val restaurant: Restaurant
) : AbstractEntity() {
    fun delete() {
        isDeleted = true
    }
}

data class RestaurantAdded(
        val accountId: String,
        val restaurantId: UUID
) : DomainEvent