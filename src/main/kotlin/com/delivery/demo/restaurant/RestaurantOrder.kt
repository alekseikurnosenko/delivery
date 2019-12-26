package com.delivery.demo.restaurant

import com.delivery.demo.DomainEvent
import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurant_orders")
class RestaurantOrder(
    @Id val restaurantId: UUID,
    status: RestaurantOrderStatus,
    @OneToOne @JoinColumn(name = "order_id") val order: Order
) : Aggregate() {

    var status: RestaurantOrderStatus = status
        protected set

    fun startPreparing() {
        if (status == RestaurantOrderStatus.Completed) {
            throw Exception("Cannot start preparing completed orders")
        }
        status = RestaurantOrderStatus.Active
        // 1. How do we update Order status now?
        // Definitely not from here
        // Via event?

        registerEvent(OrderPreparationStarted(order.id))
    }

    fun finishPreparing() {
        status = RestaurantOrderStatus.Completed

        registerEvent(OrderPreparationFinished(order.id))
    }
}

data class OrderPreparationStarted(val orderId: UUID) : DomainEvent

data class OrderPreparationFinished(val orderId: UUID) : DomainEvent

enum class RestaurantOrderStatus {
    Pending,
    Active,
    Completed
}

abstract class Aggregate {
    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()


    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    val events: List<DomainEvent> = domainEvents
}