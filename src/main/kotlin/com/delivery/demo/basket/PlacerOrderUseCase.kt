package com.delivery.demo.basket

import com.delivery.demo.courier.Courier
import com.delivery.demo.courier.CourierLocationRepository
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.courier.LatLng
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.OrderStatus
import com.delivery.demo.payment.PaymentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlacerOrderUseCase(
    val basketRepository: BasketRepository,
    val courierRepository: CourierRepository,
    val orderRepository: OrderRepository,
    val courierLocationRepository: CourierLocationRepository,
    val paymentService: PaymentService
) {

    fun place(basket: Basket): Order {
        val restaurant = basket.restaurant
        // Ensure it's still working
        if (!restaurant.isAcceptingOrders) {
            throw Exception("Restaurant is not accepting orders")
        }
        // List all couriers that are on shift
        val couriers = courierRepository.findByOnShift(true)
        // Ensure that there are some available
        if (couriers.isEmpty()) {
            throw Exception("No courier available")
        }

        // Create order
        val order = restaurant.placeOrder(
            userId = basket.owner,
            deliveryAddress = basket.deliveryAddress,
            items = basket.items
        )
        orderRepository.save(order)

        // Charge user
        paymentService.charge(basket.owner, order.totalAmount)
        // Which aggregate are we supposed to publish this event to?
        // Order?

        // What if we crash? 🤔
        val courier = findCourier(
            pickupLocation = basket.restaurant.address.location,
            deliveryLocation = basket.deliveryAddress.location
        )

        if (courier == null) {
            // FIXME What if refund fails due to network error 🤔
            paymentService.refund(basket.owner, order.totalAmount)
            throw Exception("No courier available")
        }

        // Assign
        val courierOrder = courier.assignOrder(order)
        // 🤔 So why are we supposed to save courier here?
        // while returning an order?
        // And I still don't like that it happens in two steps
        courierRepository.save(courier)


        // Clear the basket
        basketRepository.delete(basket)

        return order
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


        return courierRepository.findAllByIdAndOnShift(candidates.map { it.key })
            .map { courier ->
                courier to candidates.first { it.key == courier.id }.value
            }
            .minBy { (courier, location) ->
                val remainingTime = courier.activeOrders.sumByDouble { order ->
                    when (order.status) {
                        OrderStatus.Placed,
                        OrderStatus.Preparing,
                        OrderStatus.AwaitingPickup -> {
                            location.latLng.distanceTo(pickupLocation) +
                                    pickupLocation.distanceTo(order.deliveryAddress.location)
                        }
                        OrderStatus.InDelivery -> {
                            location.latLng.distanceTo(order.deliveryAddress.location)
                        }
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