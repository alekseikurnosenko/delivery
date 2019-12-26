package com.delivery.demo.order

import com.delivery.demo.Address
import com.delivery.demo.courier.Courier
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "orders")
data class Order constructor(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    val restaurant: Restaurant,
    @ManyToOne
    @JoinColumn(name = "courier_id")
    private var courier: Courier? = null,
    val deliveryAddress: Address,
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: MutableList<OrderItem> = mutableListOf(),
    private var _status: OrderStatus
) {
    val status: OrderStatus
        get() = _status

    fun startPreparing() {
        _status = OrderStatus.Preparing
    }

    fun completePreparing() {
        _status = OrderStatus.AwaitingPickup
    }

    fun assignToCourier(courier: Courier) {
        this.courier = courier
    }

    override fun toString(): String {
        return "${Order::class.java.simpleName}$id"
    }

    fun confirmPickup() {
        _status = OrderStatus.InDelivery
    }

    fun confirmDropoff() {
        _status = OrderStatus.Delivered
    }

    companion object {
        fun place(
            restaurant: Restaurant,
            deliveryAddress: Address,
            items: MutableList<OrderItem>
        ): Order {
            return Order(
                restaurant = restaurant,
                deliveryAddress = deliveryAddress,
                items = items,
                _status = OrderStatus.Placed
            )
        }
    }
}

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne
    @JoinColumn(name = "dish_id")
    val dish: Dish,
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    val order: Order,
    val quantity: Int
) {
    override fun toString(): String {
        return "${OrderItem::class.java.simpleName}$id"
    }
}

enum class OrderStatus {
    Placed,
    Paid,
    Preparing, // Restaurant has started preparing the order
    AwaitingPickup, // Restaurant has finsihed preparing and is awaiting courier arrival
    InDelivery, // Courier has picked up an order and is on the way to client
    Delivered, // Order was successfuly delivered to client
}