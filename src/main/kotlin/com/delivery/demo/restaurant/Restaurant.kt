package com.delivery.demo.restaurant

import com.delivery.demo.Address
import com.delivery.demo.basket.Basket
import com.delivery.demo.order.Order
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id
    val id: UUID,
    @field:NotNull
    val name: String,
    @OneToMany(mappedBy = "restaurant", cascade = [CascadeType.ALL])
    private val _dishes: MutableList<Dish> = mutableListOf(),
    val minimumOrderAmount: Money? = null,
    @Embedded
    val address: Address,
    val currency: CurrencyUnit
) {

    val isAcceptingOrders: Boolean
        get() = true // TOOD: operating times?

    val dishes: List<Dish>
        @NotNull
        get() = _dishes


    fun addDish(name: String, price: Money): Dish {
        val dish = Dish(
            id = UUID.randomUUID(),
            name = name,
            price = price,
            restaurant = this
        )
        _dishes.add(dish)
        return dish
    }

    fun startPreparing(order: Order) {


        // Emit an event order added?
    }

    fun completePreparing(order: Order) {
        // Where to save it?
//        order.copy(status = OrderStatus.AwaitingPickup)

        // Emit an event order added?
    }

    fun newBasket(owner: String): Basket {
        return Basket(UUID.randomUUID(), owner, this)
    }

    fun placeOrder(order: Order): RestaurantOrder {
        if (!isAcceptingOrders) {
            throw Exception("Cannot place order since the restaurant is not accepting any")
        }
        return RestaurantOrder(id, RestaurantOrderStatus.Pending, order)
    }

}

@Entity
class Dish(
    @Id
    val id: UUID,
    val name: String,
    val price: Money,
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    val restaurant: Restaurant
)