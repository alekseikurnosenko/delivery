package com.delivery.demo.basket

import com.delivery.restaurant.model.Dish
import com.delivery.restaurant.model.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.money.Money
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "baskets")
data class Basket(
    @Id
    val id: UUID,
    val owner: String,
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    val restaurant: Restaurant,
    @OneToMany(mappedBy = "basket", cascade = [CascadeType.ALL])
    private val _items: MutableList<BasketItem> = mutableListOf()
) {

    val items: List<BasketItem>
        get() = _items

    val totalAmount: Money
        get() = if (_items.isEmpty()) {
            Money.of(restaurant.currency, BigDecimal.ZERO)
        } else {
            _items.map { it.dish.price.multipliedBy(it.quantity.toLong()) }.reduce { acc, money ->
                acc + money
            }
        }

    val isAboveMinimumOrder: Boolean
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
            _items.add(
                BasketItem(
                    dish = dish,
                    quantity = quantity,
                    basket = this
                )
            )
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
        if (!isAboveMinimumOrder) {
            throw Exception("Cannot checkout: below minimum order")
        }

        // Generate PaymentIntent using `totalAmount`
        // Send it to client?
//        status = WaitingForPayment.

    }

    override fun toString(): String {
        return "${Basket::class.java.simpleName}(id=$id)"
    }
}

@Entity
@Table(name = "basket_items")
data class BasketItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne
    @JoinColumn(name = "dish_id")
    val dish: Dish,
    val quantity: Int,
    @ManyToOne
    @JoinColumn(name = "basket_id")
    val basket: Basket
) {
    override fun toString(): String {
        return "${BasketItem::class.simpleName}(id=$id)"
    }
}