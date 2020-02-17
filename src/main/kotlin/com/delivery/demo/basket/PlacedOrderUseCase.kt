package com.delivery.demo.basket

import com.delivery.demo.EventPublisher
import com.delivery.demo.courier.Courier
import com.delivery.demo.courier.CourierLocationRepository
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.courier.LatLng
import com.delivery.demo.order.OrderPaid
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.OrderStatus
import com.delivery.demo.payment.PaymentService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlacedOrderUseCase(
    val basketRepository: BasketRepository,
    val courierRepository: CourierRepository,
    val orderRepository: OrderRepository,
    val courierLocationRepository: CourierLocationRepository,
    val paymentService: PaymentService,
    val eventPublisher: EventPublisher
) {


    @Transactional
    @RabbitListener(queues = ["order.paid"])
    fun onOrderPaid(orderPaid: OrderPaid) {
        val order = orderRepository.findById(orderPaid.orderId)
            .orElseThrow { Exception("Order(id=${orderPaid.orderId} not found") }

        val courier = findCourier(
            pickupLocation = order.restaurant.address.location,
            deliveryLocation = order.deliveryAddress.location
        )

        if (courier == null) {
            // Assume that it's idempotent
            paymentService.refund(order.userId, "TODO", order.totalAmount)

            // order.cancel?
            // Publish event?
            return
        }

        val assignedOrder = courier.assignOrder(order)
        // 🤔 So why are we supposed to save courier here?
        // while returning an order?
        // And I still don't like that it happens in two steps
        courierRepository.save(courier)
        eventPublisher.publish(assignedOrder.events)
    }

    private fun findCourier(pickupLocation: LatLng, deliveryLocation: LatLng): Courier? {
        // Find the closest courier to the resturant
        // So, it should be the minimum of
        // sum (time to finish current order + time to pickup new + time to drop )
        // NOTE: initial implementation of finding the best possible courier is very slow
        // due to N queries to fetch active orders
        // Instead - find 10 closest and check them
        val candidates = courierLocationRepository.getAll()
            .sortedBy { (_, location) ->
                location.latLng.distanceTo(pickupLocation)
            }
            .take(10)


        return courierRepository.findByIdInAndOnShift(candidates.map { it.key })
            .map { courier ->
                courier to candidates.first { it.key == courier.id }.value
            }
            .minBy { (courier, location) ->
                val remainingTime = courier.activeOrders.sumByDouble { order ->
                    when (order.status) {
                        OrderStatus.Placed,
                        OrderStatus.SentToRestaurant,
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