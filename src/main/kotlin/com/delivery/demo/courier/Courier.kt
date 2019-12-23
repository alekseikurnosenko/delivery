package com.delivery.demo.courier

import com.delivery.demo.order.Order
import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.*
import javax.persistence.*

@Embeddable
data class LatLng(
    val latitude: Float,
    val longitude: Float
)

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
    private var location: LocationReport,
    private var onShift: Boolean,
    @OneToMany(mappedBy = "courier")
    @JsonIgnore // Otherwise we have a cycle!
    private var _orders: MutableList<Order> = mutableListOf()
) {

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
        this.location = location
    }

    override fun toString(): String {
        return "${Courier::class.java.simpleName}$id"
    }
}