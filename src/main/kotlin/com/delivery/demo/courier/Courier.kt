package com.delivery.demo.courier

import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
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
    val pendingDeliveryRequests: MutableSet<DeliveryRequest> = mutableSetOf()

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

    fun requestDelivery(deliveryRequest: DeliveryRequest) {
        pendingDeliveryRequests.add(deliveryRequest)
    }

    fun onDeliveryRequestRejected(order: Order) {
        pendingDeliveryRequests.removeAll { it.delivery.order == order }
    }

    fun onDeliveryRequestAccepted(order: Order) {
        pendingDeliveryRequests.removeAll { it.delivery.order == order }
    }

    fun onDeliveryRequestTimedOut(order: Order) {
        pendingDeliveryRequests.removeAll { it.delivery.order == order }
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