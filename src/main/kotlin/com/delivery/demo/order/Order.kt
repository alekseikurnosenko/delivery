package com.delivery.demo.order

import com.delivery.demo.AbstractEntity
import com.delivery.demo.Address
import com.delivery.demo.DomainEvent

import com.delivery.demo.basket.BasketItem
import com.delivery.demo.courier.Courier
import com.delivery.demo.delivery.Delivery
import com.delivery.demo.delivery.DeliveryRequest
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.annotations.Where
import org.joda.money.Money
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "orders")
@Where(clause = "is_deleted = false")
class Order private constructor(
        val userId: String,
        val deliveryAddress: Address,
        val totalAmount: Money,
        @ManyToOne @JoinColumn(name = "restaurant_id") val restaurant: Restaurant
) : AbstractEntity() {

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL])
    @JvmSuppressWildcards // Needed due to "Collection has neither generic type or OneToMany.targetEntity()" error
    lateinit var items: List<OrderItem>
        protected set

    @OneToOne(mappedBy = "order", cascade = [CascadeType.ALL])
    lateinit var delivery: Delivery
        protected set

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.Placed
        protected set

    fun startPreparing() {
        if (status == OrderStatus.Preparing) {
            // Idempotent
            return
        }
        if (status == OrderStatus.Paid) {
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

    /**
     * TODO: Rename to assignToCourier?
     * Set assignedCourier here!
     */
    fun onAssignedToCourier(courier: Courier) {
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

    fun confirmPickup(courier: Courier) {
        if (delivery.assignedCourier != courier) {
            throw Exception("$this is not assigned to $courier")
        }
        status = OrderStatus.InDelivery

        registerEvent(OrderPickedUp(id, status))
    }

    fun confirmDropoff(courier: Courier) {
        if (delivery.assignedCourier != courier) {
            throw Exception("$this is not assigned to $courier")
        }
        status = OrderStatus.Delivered

        registerEvent(OrderDelivered(id, status))
    }

    fun confirmPaid() {
        status = OrderStatus.Paid

        registerEvent(
            OrderPaid(
                userId,
                id,
                restaurant.id,
                restaurant.name,
                totalAmount,
                deliveryAddress,
                status
            )
        )
    }

    fun cancel(reason: String) {
        status = OrderStatus.Canceled

        registerEvent(
                OrderCanceled(
                        id,
                        status,
                        reason
                )
        )
    }

    fun acceptDeliveryRequest(courier: Courier): DeliveryRequest {
        return delivery.acceptRequestAsCourier(courier)
    }

    fun rejectDeliveryRequest(courier: Courier): DeliveryRequest {
        return delivery.rejectRequestAsCourier(courier)
    }

    fun timeoutDeliveryRequest(courier: Courier) {
        delivery.timeoutRequest(courier)
    }

    override val events: List<DomainEvent>
        get() = super.events + (delivery.events ?: listOf())

    companion object {
        fun place(
            userId: String,
            restaurant: Restaurant,
            deliveryAddress: Address,
            items: List<BasketItem>
        ): Order {
            val totalAmount = items.map { it.dish.price.multipliedBy(it.quantity.toLong()) }.reduce { acc, money ->
                acc + money
            }
            val order = Order(
                    userId = userId,
                    restaurant = restaurant,
                    deliveryAddress = deliveryAddress,
                    totalAmount = totalAmount
            )
            order.items = items.map { OrderItem(dish = it.dish, quantity = it.quantity, order = order) }
            order.delivery = Delivery(order)

            // Do we also need to include all items here?
            // Or like, whole Order object?
            val event = OrderPlaced(
                userId = userId,
                orderId = order.id,
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                deliveryAddress = order.deliveryAddress,
                totalAmount = order.totalAmount,
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
    Canceled,
    Placed, // Restaurant has received the order
    Paid, // ?
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

data class OrderCreated( //?
    val userId: String,
    val orderId: UUID,
    val restaurantId: UUID,
    val restaurantName: String,
    val totalAmount: Money,
    val deliveryAddress: Address,
    val status: OrderStatus
) : DomainEvent

data class OrderPlaced(
    val userId: String,
    val orderId: UUID,
    val restaurantId: UUID,
    val restaurantName: String,
    val totalAmount: Money,
    val deliveryAddress: Address,
    val status: OrderStatus
) : DomainEvent {
    companion object {
        const val queue = "order.placed"
    }
}

data class OrderPaid(
    val userId: String,
    val orderId: UUID,
    val restaurantId: UUID,
    val restaurantName: String,
    val totalAmount: Money,
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

data class OrderCanceled(
    val orderId: UUID,
    val status: OrderStatus,
    val reason: String
) : DomainEvent