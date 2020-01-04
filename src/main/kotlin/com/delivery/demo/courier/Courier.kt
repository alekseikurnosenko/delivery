package com.delivery.demo.courier

import com.delivery.demo.Aggregate
import com.delivery.demo.DomainEvent
import com.delivery.demo.order.Order
import io.swagger.v3.oas.annotations.media.Schema
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
    @Embedded
    private var _location: LocationReport? = null,
    val userId: String,
    onShift: Boolean
) : Aggregate() {

    @OneToMany(cascade = [CascadeType.MERGE])
    @JoinTable
    val activeOrders: MutableList<Order> = mutableListOf()

    var onShift: Boolean = onShift
        protected set

    val location: LocationReport?
        get() = _location

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
//        val order = CourierOrder(order.id, this, order)
        order.assignToCourier(this)
        activeOrders.add(order)
        return order
    }

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

    fun updateLocation(location: LocationReport) {
        this._location = location
        registerEvent(CourierLocationUpdated(id, location.latLng))
    }

    companion object {
        fun new(userId: String, fullName: String): Courier {
            val courier = Courier(
                userId = userId,
                fullName = fullName,
                onShift = false
            )
            courier.registerEvent(CourierAdded(courier.id, courier.fullName, courier.onShift))
            return courier
        }
    }
}

@Schema(name = "Courier")
data class CourierDTO(
    val id: String,
    val fullName: String,
    val onShift: Boolean,
    val location: LatLng?
)

fun Courier.asDTO() = CourierDTO(
    id = id.toString(),
    fullName = fullName,
    onShift = onShift,
    location = location?.latLng
)

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
    val courierId: UUID,
    val fullName: String,
    val onShift: Boolean
) : DomainEvent