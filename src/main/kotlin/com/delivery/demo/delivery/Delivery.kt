package com.delivery.demo.delivery

import com.delivery.demo.AbstractEntity
import com.delivery.demo.Address
import com.delivery.demo.DomainEvent
import com.delivery.demo.courier.Courier
import com.delivery.demo.order.Order
import java.util.*
import javax.persistence.*

/**
 * Entity within Order aggregate root
 */
@Entity
@Table(name = "deliveries")
class Delivery(
    @OneToOne
    @JoinColumn(name = "order_id")
    val order: Order
) : AbstractEntity() {

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

    @Enumerated(EnumType.STRING)
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
                orderId = order.id,
                courierId = courier.id,
                pickup = pickup,
                dropoff = dropoff
            )
        )
        return request
    }

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

        // TODO: check timeout

        request.accept()
        registerEvent(
            DeliveryRequestAccepted(
                orderId = order.id,
                courierId = courier.id
            )
        )

        // Duplicate? We already have request that was accepted?
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
                orderId = order.id,
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
                orderId = order.id,
                courierId = courier.id
            )
        )
    }
}


enum class DeliveryStatus {
    Pending,
    WaitingForCourierConfirmation,
    CourierConfirmed,
    Failed
}

data class DeliveryRequested(
    val orderId: UUID,
    val courierId: UUID,
    val pickup: Address,
    val dropoff: Address
) : DomainEvent

data class DeliveryRequestAccepted(
    val orderId: UUID,
    val courierId: UUID,
    override val routingKey: String = queue
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.accepted"
    }
}

data class DeliveryRequestRejected(
    val orderId: UUID,
    val courierId: UUID,
    override val routingKey: String = queue
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.rejected"
    }
}

data class DeliveryRequestTimedOut(
    val orderId: UUID,
    val courierId: UUID,
    override val routingKey: String = queue
) : DomainEvent {
    companion object {
        const val queue = "deliveryRequest.timed_out"
    }
}