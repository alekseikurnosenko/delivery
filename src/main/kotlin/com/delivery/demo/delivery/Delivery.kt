package com.delivery.demo.delivery

import com.delivery.demo.Address
import com.delivery.demo.Aggregate
import com.delivery.demo.courier.*
import com.delivery.demo.order.Order
import javax.persistence.*

/**
 * Aka "CourierOrder"
 */
@Entity
@Table(name = "deliveries")
class Delivery(
    @OneToOne
    @JoinColumn(name = "order_id")
    val order: Order
) : Aggregate() {

    val pickup: Address
        get() = order.restaurant.address

    val dropoff: Address
        get() = order.deliveryAddress

    @ManyToOne
    @JoinColumn(name = "courier_id")
    var assignedCourier: Courier? = null
        protected set

    @OneToMany(mappedBy = "delivery", cascade = [CascadeType.ALL])
    var requests: MutableList<DeliveryRequest> = mutableListOf()
        protected set

    var status: DeliveryStatus = DeliveryStatus.Pending
        protected set

    // Here we want to have
    // 1. courier who have rejected this request, so that we don't ask them again
    // 2. courier who have timed out, because we might to try them again some time later

    fun requestCourier(courier: Courier): DeliveryRequest {
        // Business rule: we should ask courier only once
        val existingRequest = requests.find { it.courier == courier }
        if (existingRequest != null) {
            if (existingRequest.status == DeliveryRequestStatus.Requested) {
                return existingRequest // Idempotence
            } else {
                throw Exception("$this already requested from $courier, previous request - $existingRequest")
            }
        }
        val request = DeliveryRequest(courier, this)
        requests.add(request)

        registerEvent(
            DeliveryRequested(
                requestId = request.id,
                deliveryId = id,
                courierId = courier.id,
                pickup = pickup,
                dropoff = dropoff
            )
        )
        return request
    }

    /**
     * The good:
     * DeliveryRequests are owned by Delivery
     * The weird:
     * Delivery handles both "dispatching" part and actual delivery
     */
    fun acceptRequestAsCourier(courier: Courier) {
        // Invariant can accept only requests that were requested
        val request = requests.find { it.courier == courier }
            ?: throw Exception("$this wasn't requested from $courier")

        // Invariant: cannot accept rejected/timed out requests
        if (request.status != DeliveryRequestStatus.Requested) {
            throw Exception("$this cannot accepted the request because it is ${request.status}")
        }

        // Idempotence
        if (request.status == DeliveryRequestStatus.Accepted) {
            return
        }

        request.accept()
        registerEvent(
            DeliveryRequestAccepted(
                requestId = request.id,
                deliveryId = id,
                courierId = courier.id
            )
        )

        // Duplicate? We already have acceptedCourier?
        assignedCourier = courier

        // TODO: consider revoking all other pending requests in case of asking multiple couriers at the same time?
    }

    fun rejectRequestAsCourier(courier: Courier) {
        val request = requests.find { it.courier == courier }
            ?: throw Exception("$this wasn't requested from $courier")

        // Invariant: cannot accept rejected/timed out requests
        if (request.status != DeliveryRequestStatus.Requested) {
            throw Exception("$this cannot accepted the request because it is ${request.status}")
        }
        if (request.status == DeliveryRequestStatus.Rejected) {
            return // Idempotence
        }

        request.reject()
        registerEvent(
            DeliveryRequestRejected(
                requestId = request.id,
                deliveryId = id,
                courierId = courier.id
            )
        )
    }

    fun timeoutRequest(courier: Courier) {
        val request = requests.find { it.courier == courier }
            ?: throw Exception("$this wasn't requested from $courier")

        // If request was acted upon - ignore
        if (request.status != DeliveryRequestStatus.Requested) {
            return
        }

        request.timeout()
        registerEvent(
            DeliveryRequestTimedOut(
                requestId = request.id,
                deliveryId = id,
                courierId = courier.id
            )
        )
    }

    fun confirmPickupAsCourier(courier: Courier) {
        if (assignedCourier != courier) {
            throw Exception("Delivery(id=$id) does not belong to Courier(id=${courier.id})")
        }

        status = DeliveryStatus.OrderPickedUp

//        registerEvent(
//            DeliveryPickedUp()
//        )
    }

}


enum class DeliveryStatus {
    Pending,
    WaitingForCourierConfirmation,
    CourierConfirmed,
    OrderPickedUp,
    OrderDelivered,
    Failed
}


@Entity
@Table(name = "delivery_requests")
class DeliveryRequest(
    @ManyToOne
    @JoinColumn(name = "courier_id")
    val courier: Courier,
    @ManyToOne
    @JoinColumn(name = "delivery_id")
    val delivery: Delivery
) : Aggregate() {

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