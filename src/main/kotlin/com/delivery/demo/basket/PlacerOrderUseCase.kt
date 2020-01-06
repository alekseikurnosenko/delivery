package com.delivery.demo.basket

import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.OrderStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PlacerOrderUseCase(
    val basketRepository: BasketRepository,
    val courierRepository: CourierRepository,
    val orderRepository: OrderRepository
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

        // Find the closest courier to the resturant
        // So, it should be the minimum of
        // sum (time to finish current order + time to pickup new + time to drop )
        // TODO: does it make sense to allow couriers without location reported?
        val courier = couriers
            .filter { it.location != null }
            .minBy { courier ->
                val remainingTime = courier.activeOrders.sumByDouble { order ->
                    when (order.status) {
                        OrderStatus.Placed,
                        OrderStatus.Preparing,
                        OrderStatus.AwaitingPickup -> {
                            courier.location!!.latLng.distanceTo(restaurant.address.location) +
                                    restaurant.address.location.distanceTo(order.deliveryAddress.location)
                        }
                        OrderStatus.InDelivery -> {
                            courier.location!!.latLng.distanceTo(order.deliveryAddress.location)
                        }
                        OrderStatus.Delivered -> 0.0
                    }
                }
                val newEta = courier.location!!.latLng.distanceTo(restaurant.address.location) +
                        restaurant.address.location.distanceTo(basket.deliveryAddress.location)

                // Use a coefficient to penalize pending orders
                remainingTime * 1.2 + newEta
            }
            ?: throw Exception("No courier available")

        // TODO: Charge payment

        // Create order
        val order = restaurant.placeOrder(
            userId = basket.owner,
            deliveryAddress = basket.deliveryAddress,
            items = basket.items
        )
//        // Not sure about this one
//        // Set courier
//        // NOTE: currently this part emits event!
//        order.assignToCourier(courier)
//        // >< since we save entity, transient fields dissapear?
        orderRepository.save(order)

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
}