package com.delivery.demo.courier

import com.delivery.demo.order.Order
import com.fasterxml.jackson.annotation.JsonIgnore
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
data class Courier(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fullName: String,
    @Embedded
    private var _location: LocationReport? = null,
    private var onShift: Boolean,
    @OneToMany(mappedBy = "courier")
    @JsonIgnore // Otherwise we have a cycle!
    private var _orders: MutableList<Order> = mutableListOf()
) {

    val location: LocationReport?
        get() = _location

    val orders: List<Order>
        get() = _orders

    fun addOrder(order: Order) {
        _orders.add(order)
    }

    fun startShift() {
        onShift = true
    }

    fun stopShift() {
        onShift = false
    }

    fun updateLocation(location: LocationReport) {
        this._location = location
    }

    override fun toString(): String {
        return "${Courier::class.java.simpleName}$id"
    }

    companion object {
        fun new(fullName: String): Courier = Courier(
            fullName = fullName,
            onShift = false
        )
    }
}

data class CourierDTO(
    val id: String
)

fun Courier.asDTO() = CourierDTO(
    id = id.toString()
)