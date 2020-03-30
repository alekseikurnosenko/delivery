package com.delivery.demo.delivery

import com.delivery.demo.EventPublisher
import com.delivery.demo.courier.Courier
import com.delivery.demo.courier.CourierLocationRepository
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.courier.LatLng
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.OrderStatus
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class DeliveryService(
    val courierRepository: CourierRepository,
    val courierLocationRepository: CourierLocationRepository,
    val taskScheduler: TaskScheduler,
    val eventPublisher: EventPublisher,
    val orderRepository: OrderRepository
) {

    val requestTimeoutSeconds: Long = 30 // 30 seconds

    /**
     * So, I envision this service to be the one finding candidate couriers and asking them.
     * Also the one scheduling timeouts?
     * ???
     * 1. How to know that the courier has accepted a request? Subscribe to an event here?
     * 2. How to short-circuit event processing if there are no courier available?
     * 3. What do we use delivery for? I suppose it would be our state? Is it an aggregate then?
     * How do we link to an order? Should we return it? And call this "scheduleDeliveryForOrder"?
     */

    fun findCourierForOrder(order: Order) {
        tryRequestCourier(order.delivery)
    }

    private fun tryRequestCourier(delivery: Delivery) {
        val courier = findCourier(
            pickupLocation = delivery.pickup.location,
            deliveryLocation = delivery.dropoff.location,
            couriersToIgnore = delivery.requests.map { it.courier.id }
        )
        if (courier == null) {
            // No more couriers available
            // At this point, we are saying that the order failed
            // Or maybe mark delivery as failed?
            delivery.order.cancel("No couriers available")
            eventPublisher.publish(delivery.order.events)
            return
        }
        val request = delivery.requestCourier(courier)
        courier.requestDelivery(request)

        eventPublisher.publish(delivery.events)
        eventPublisher.publish(courier.events)


        // Should we keep candidate couriers?
        // Or just get the fresh ones every time? <-- makes more sense

        // Schedule a timeout
        // TODO: in-memory only, integrate Quartz instead
        taskScheduler.schedule({
            // Not transactional because Spring magic again!
            handleTimeout(delivery.order.id, courier.id)
        }, Instant.now().plusSeconds(requestTimeoutSeconds))
    }

    @Transactional
    fun handleTimeout(orderId: UUID, courierId: UUID) {
        val order = orderRepository.findById(orderId).get()
        val courier = courierRepository.findById(courierId).get()
        order.timeoutDeliveryRequest(courier)
        courier.onDeliveryRequestTimedOut(order)

        eventPublisher.publish(order.events)
        eventPublisher.publish(courier.events)
    }

    @RabbitListener(queues = [DeliveryRequestAccepted.queue])
    @Transactional
    fun onDeliveryRequestAccepted(event: DeliveryRequestAccepted) {
        val order = orderRepository.findById(event.orderId).orElseThrow {
            AmqpRejectAndDontRequeueException("Unknown Order(id=${event.orderId})")
        }
        val courier = courierRepository.findById(event.courierId).orElseThrow {
            AmqpRejectAndDontRequeueException("Unknown Courier(id=${event.courierId})")
        }

        order.onAssignedToCourier(courier)
        eventPublisher.publish(order.events)
    }

    @RabbitListener(queues = [DeliveryRequestRejected.queue])
    @Transactional
    fun onDeliveryRequestRejected(event: DeliveryRequestRejected) {
        val order = orderRepository.findById(event.orderId).orElseThrow {
            AmqpRejectAndDontRequeueException("Unknown Order(id=${event.orderId})")
        }

        tryRequestCourier(order.delivery)
    }

    @RabbitListener(queues = [DeliveryRequestTimedOut.queue])
    @Transactional
    fun onDeliveryRequestTimedOut(event: DeliveryRequestTimedOut) {
        val order = orderRepository.findById(event.orderId).orElseThrow {
            AmqpRejectAndDontRequeueException("Unknown Order(id=${event.orderId})")
        }

        tryRequestCourier(order.delivery)
    }

    private fun findCourier(
        pickupLocation: LatLng,
        deliveryLocation: LatLng,
        couriersToIgnore: List<UUID>
    ): Courier? {
        // Find the closest courier to the restaurant
        // So, it should be the minimum of
        // sum (time to finish current order + time to pickup new + time to drop )
        // NOTE: initial implementation of finding the best possible courier is very slow
        // due to N queries to fetch active orders
        // Instead - find 10 closest and check them
        // It's still very slow
        // Ideally, when we fetch couriers, we have to fetch them together with all orders
        // To reduce amount of database calls
        val candidates = courierLocationRepository.getAll()
            .filter { (courierId, _) -> !couriersToIgnore.contains(courierId) }
            .sortedBy { (_, location) ->
                location.latLng.distanceTo(pickupLocation)
            }
            .take(10)


        // What should we do with the couriers currently being asked?
        // Skip?

        return courierRepository.findByIdInAndOnShift(candidates.map { it.key })
            .map { courier ->
                courier to candidates.first { it.key == courier.id }.value
            }
            .minBy { (courier, location) ->
                val remainingTime = courier.activeOrders.sumByDouble { order ->
                    when (order.status) {
                        OrderStatus.Placed,
                        OrderStatus.Paid,
                        OrderStatus.Preparing,
                        OrderStatus.AwaitingPickup -> {
                            location.latLng.distanceTo(pickupLocation) +
                                    pickupLocation.distanceTo(order.deliveryAddress.location)
                        }
                        OrderStatus.InDelivery -> {
                            location.latLng.distanceTo(order.deliveryAddress.location)
                        }
                        OrderStatus.Canceled,
                        OrderStatus.Delivered -> 0.0
                    }
                }
                val newEta = location.latLng.distanceTo(pickupLocation) +
                        pickupLocation.distanceTo(deliveryLocation)

                // Use a coefficient to penalize pending orders
                remainingTime * 1.2 + newEta
            }
            ?.let { (courier, _) -> courier }
    }
}