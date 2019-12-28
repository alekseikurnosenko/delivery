package com.delivery.demo.restaurant

import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurant_orders")
class RestaurantOrder(
    @Id val restaurantId: UUID,
    @OneToOne @JoinColumn(name = "order_id") val order: Order
) : Aggregate() {

    fun startPreparing() {
        order.startPreparing()
    }

    fun finishPreparing() {
        order.finishPreparing()
    }

    override val events: List<DomainEvent>
        get() = order.events
}
