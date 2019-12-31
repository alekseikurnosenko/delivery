package com.delivery.demo.order

import com.delivery.demo.Address
import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.basket.BasketItem
import com.delivery.demo.courier.Courier
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "orders")
class Order constructor(
    @Id
    val id: UUID = UUID.randomUUID(),
    restaurant: Restaurant,
    deliveryAddress: Address,
    items: List<BasketItem> // Can be an interface?
) : Aggregate() {

    val deliveryAddress: Address = deliveryAddress

    @ManyToOne
    @JoinColumn(name = "restaurant_id")
    val restaurant: Restaurant = restaurant

    @ManyToOne
    @JoinColumn(name = "courier_id")
    private var courier: Courier? = null

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: List<OrderItem> = items.map { OrderItem(dish = it.dish, quantity = it.quantity, order = this) }

    var status: OrderStatus = OrderStatus.Placed
        protected set

    fun startPreparing() {
        if (status == OrderStatus.Preparing) {
            // Idempotent
            return
        }
        if (status == OrderStatus.Placed) {
            status = OrderStatus.Preparing
            registerEvent(OrderPreparationStarted(id))
            return
        }

        throw Exception("Cannot start preparing, order is $status")
    }

    fun finishPreparing() {
        if (status == OrderStatus.AwaitingPickup) {
            // Idempotent
            return
        }
        if (status == OrderStatus.Preparing) {
            status = OrderStatus.AwaitingPickup
            registerEvent(OrderPreparationFinished(id))
            return
        }

        throw Exception("Cannot finish preparing, order is $status")
    }

    fun assignToCourier(courier: Courier) {
        this.courier = courier
        registerEvent(OrderAssigned(id, courier.id))
    }

    override fun toString(): String {
        return "${Order::class.java.simpleName}$id"
    }

    fun confirmPickup() {
        status = OrderStatus.InDelivery
    }

    fun confirmDropoff() {
        status = OrderStatus.Delivered
    }

    companion object {
        fun place(
            restaurant: Restaurant,
            deliveryAddress: Address,
            items: List<BasketItem>
        ): Order {
            val order = Order(
                restaurant = restaurant,
                deliveryAddress = deliveryAddress,
                items = items
            )

            // Do we also need to include all items here?
            // Or like, whole Order object?
            val event = OrderPlaced(
                orderId = order.id,
                restaurantId = restaurant.id
            )
            order.registerEvent(event)

            return order
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
    //    Paid,
    Preparing, // Restaurant has started preparing the order
    AwaitingPickup, // Restaurant has finsihed preparing and is awaiting courier arrival
    InDelivery, // Courier has picked up an order and is on the way to client
    Delivered, // Order was successfuly delivered to client
}

data class OrderPreparationStarted(val orderId: UUID) : DomainEvent

data class OrderPreparationFinished(val orderId: UUID) : DomainEvent

data class OrderPlaced(val orderId: UUID, val restaurantId: UUID) : DomainEvent

data class OrderAssigned(val orderId: UUID, val courierId: UUID) : DomainEvent