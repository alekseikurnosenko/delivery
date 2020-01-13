package com.delivery.demo.order

import com.delivery.demo.Address
import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.basket.BasketItem
import com.delivery.demo.courier.Courier
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import com.delivery.demo.restaurant.asDTO
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.joda.money.Money
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "orders")
class Order constructor(
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
    var courier: Courier? = null
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    val items: List<OrderItem> = items.map { OrderItem(dish = it.dish, quantity = it.quantity, order = this) }

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.Placed
        protected set

    val totalAmount: Money
        get() = items.map { it.dish.price.multipliedBy(it.quantity.toLong()) }.reduce { acc, money ->
            acc + money
        }

    fun startPreparing() {
        if (status == OrderStatus.Preparing) {
            // Idempotent
            return
        }
        if (status == OrderStatus.Placed) {
            status = OrderStatus.Preparing
            registerEvent(OrderPreparationStarted(id, status))
            return
        }

        throw Exception("Cannot start preparing Order(id=$id), order is $status")
    }

    fun finishPreparing() {
        if (status == OrderStatus.AwaitingPickup) {
            // Idempotent
            return
        }
        if (status == OrderStatus.Preparing) {
            status = OrderStatus.AwaitingPickup
            registerEvent(OrderPreparationFinished(id, status))
            return
        }

        throw Exception("Cannot finish preparing Order(id=$id), order is $status")
    }

    fun assignToCourier(courier: Courier) {
        this.courier = courier
        val event = OrderAssigned(
            orderId = id,
            courierId = courier.id,
            courierFullName = courier.fullName,
            restaurantId = restaurant.id,
            restaurantName = restaurant.name,
            restaurantAddress = restaurant.address,
            deliveryAddress = deliveryAddress,
            status = status
        )
        registerEvent(event)
    }

    fun confirmPickup() {
        status = OrderStatus.InDelivery

        registerEvent(OrderPickedUp(id, status))
    }

    fun confirmDropoff() {
        status = OrderStatus.Delivered

        registerEvent(OrderDelivered(id, status))
    }

    companion object {
        fun place(
            userId: String,
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
                userId = userId,
                orderId = order.id,
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                deliveryAddress = order.deliveryAddress,
                totalAmount = order.totalAmount.asDTO(),
                status = order.status
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

@Schema(enumAsRef = true) // TODO: Should use DTO type instead
enum class OrderStatus {
    Placed,
    //    Paid,
    Preparing, // Restaurant has started preparing the order
    AwaitingPickup, // Restaurant has finsihed preparing and is awaiting courier arrival
    InDelivery, // Courier has picked up an order and is on the way to client
    Delivered, // Order was successfuly delivered to client
}

data class OrderPreparationStarted(
    val orderId: UUID,
    val status: OrderStatus
) : DomainEvent

data class OrderPreparationFinished(
    val orderId: UUID,
    val status: OrderStatus
) : DomainEvent

data class OrderPlaced(
    val userId: String,
    val orderId: UUID,
    val restaurantId: UUID,
    val restaurantName: String,
    val totalAmount: JacksonConfiguration.MoneyView,
    val deliveryAddress: Address,
    val status: OrderStatus
) : DomainEvent

data class OrderAssigned(
    val orderId: UUID,
    val courierId: UUID,
    val courierFullName: String,
    val restaurantId: UUID,
    val restaurantName: String,
    val restaurantAddress: Address,
    val deliveryAddress: Address,
    val status: OrderStatus
) : DomainEvent

data class OrderPickedUp(
    val orderId: UUID,
    val status: OrderStatus
) : DomainEvent

data class OrderDelivered(
    val orderId: UUID,
    val status: OrderStatus
) : DomainEvent