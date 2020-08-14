package com.delivery.demo.delivery

import com.delivery.demo.AbstractEntity

import com.delivery.demo.courier.Courier
import org.hibernate.annotations.Where
import javax.persistence.*

@Entity
@Table(name = "delivery_requests")
@Where(clause = "is_deleted = false")
class DeliveryRequest(
    @ManyToOne
    @JoinColumn(name = "courier_id")
    val courier: Courier,
    @ManyToOne
    @JoinColumn(name = "delivery_id")
    val delivery: Delivery
) : AbstractEntity() {

    @Enumerated(EnumType.STRING)
    var status: DeliveryRequestStatus = DeliveryRequestStatus.Requested
        protected set

    fun accept() {
        status = DeliveryRequestStatus.Accepted
        // TODO events?
    }

    fun reject() {
        status = DeliveryRequestStatus.Rejected
        // TODO events?
    }

    fun timeout() {
        status = DeliveryRequestStatus.TimedOut
    }
}

enum class DeliveryRequestStatus {
    Requested,
    Accepted,
    Rejected,
    TimedOut
}