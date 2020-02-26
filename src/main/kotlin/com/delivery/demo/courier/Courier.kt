package com.delivery.demo.courier

import com.delivery.demo.Address
import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.delivery.Delivery
import com.delivery.demo.delivery.DeliveryRequest
import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*
import kotlin.math.sqrt

@Embeddable
data class LatLng(
    @Column(nullable = true)
    val latitude: Float,
    @Column(nullable = true)
    val longitude: Float
) {
    fun distanceTo(location: LatLng): Double {
        // Lol whatever
        val latDiff = location.latitude - latitude
        val lonDiff = location.longitude - longitude
        val distanceSquared = latDiff * latDiff + lonDiff * lonDiff
        return sqrt(distanceSquared.toDouble())
    }
}

@Embeddable
data class LocationReport(
    @Embedded
    val latLng: LatLng,
    val timestamp: Date
)

@Entity
@Table(name = "couriers")
class Courier(
    val fullName: String,
    val userId: String,
    onShift: Boolean
) : Aggregate() {

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinTable
    val activeOrders: MutableSet<Order> = mutableSetOf()

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinTable
    val pendingDeliveryRequests: MutableList<DeliveryRequest> = mutableListOf()

    var onShift: Boolean = onShift
        protected set

    fun startShift() {
        onShift = true
        registerEvent(CourierShiftStarted(id))
    }

    fun stopShift() {
        onShift = false
        // What to do with existing orders?
        // We can assume that couriers off-shift wouldn't get any more orders, but have to complete active ones
        registerEvent(CourierShiftStopped(id))
    }

    fun assignOrder(order: Order): Order {
        if (!onShift) {
            throw Exception("Cannot assign orders to couriers off-shift")
        }
        order.assignToCourier(this)
        activeOrders.add(order)
        return order
    }

    // confirmDeliveryPickup
    // Not part of courier!
    fun confirmOrderPickup(orderId: UUID): Order {
        val order = activeOrders.find { it.id == orderId } ?: throw Exception("Unknown order: $orderId")
        order.confirmPickup()
        return order
    }

    fun confirmOrderDropoff(orderId: UUID): Order {
        val order = activeOrders.find { it.id == orderId } ?: throw Exception("Unknown order: $orderId")
        order.confirmDropoff()
        activeOrders.remove(order)
        return order
    }

    fun requestDelivery(deliveryRequest: DeliveryRequest) {
        pendingDeliveryRequests.add(deliveryRequest)
    }

    fun acceptRequest(delivery: Delivery) {
        pendingDeliveryRequests.removeAll { it.delivery.id == delivery.id }
    }

    fun rejectRequest(delivery: Delivery) {
        pendingDeliveryRequests.removeAll { it.delivery.id == delivery.id }
    }

    fun timeoutRequest(delivery: Delivery) {
        pendingDeliveryRequests.removeAll { it.delivery.id == delivery.id }
    }


    companion object {
        fun new(accountId: String, fullName: String): Courier {
            val courier = Courier(
                userId = accountId,
                fullName = fullName,
                onShift = false
            )
            courier.registerEvent(
                CourierAdded(
                    accountId = accountId,
                    courierId = courier.id,
                    fullName = courier.fullName,
                    onShift = courier.onShift
                )
            )
            return courier
        }
    }
}

data class CourierLocationUpdated(
    val courierId: UUID,
    val location: LatLng
) : DomainEvent

data class CourierShiftStarted(
    val courierId: UUID
) : DomainEvent

data class CourierShiftStopped(
    val courierId: UUID
) : DomainEvent

data class CourierAdded(
    val accountId: String,
    val courierId: UUID,
    val fullName: String,
    val onShift: Boolean
) : DomainEvent

data class DeliveryRequested(
    val requestId: UUID,
    val deliveryId: UUID,
    val courierId: UUID,
    val pickup: Address,
    val dropoff: Address
) : DomainEvent

data class DeliveryRequestAccepted(
    val requestId: UUID,
    val deliveryId: UUID,
    val courierId: UUID
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.accepted"
    }
}

data class DeliveryRequestRejected(
    val requestId: UUID,
    val deliveryId: UUID,
    val courierId: UUID
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.rejected"
    }
}

data class DeliveryRequestTimedOut(
    val requestId: UUID,
    val deliveryId: UUID,
    val courierId: UUID
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.timed_out"
    }
}