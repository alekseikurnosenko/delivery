package com.delivery.demo.courier

import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "courier_orders")
class CourierOrder(
    @Id
    val id: UUID,
    @ManyToOne @JoinColumn(name = "courier_id") val courier: Courier,
    @OneToOne @JoinColumn(name = "order_id") val order: Order
) : Aggregate() {
    fun confirmPickup() {
        order.confirmPickup()
    }

    fun confirmDropoff() {
        order.confirmDropoff()
    }

    override val events: List<DomainEvent>
        get() = order.events
}