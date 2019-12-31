package com.delivery.demo.basket

import com.delivery.demo.Address
import com.delivery.demo.courier.CourierOrderRepository
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.restaurant.RestaurantOrderRepository
import org.springframework.stereotype.Service

@Service
class PlacerOrderUseCase(
    val basketRepository: BasketRepository,
    val courierRepository: CourierRepository,
    val restaurantOrderRepository: RestaurantOrderRepository,
    val orderRepository: OrderRepository,
    val courierOrderRepository: CourierOrderRepository
) {

    fun place(basket: Basket, deliveryAddress: Address): Order {
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
        // TODO: does it make sense to allow couriers without location reported?
        val courier = couriers
            .filter { it.location != null }
            // FIXME: need to filter only free couriers!
            .minBy { it.location!!.latLng.distanceTo(restaurant.address.location) }
            ?: throw Exception("No courier available")

        // TODO: Charge payment

        // Create order
        val order = Order.place(
            restaurant = basket.restaurant,
            deliveryAddress = deliveryAddress,
            items = basket.items
        )
        // Not sure about this one
        // Set courier
        // NOTE: currently this part emits event!
        order.assignToCourier(courier)
        // >< since we save entity, transient fields dissapear?
        orderRepository.save(order)

        // Assign
        val courierOrder = courier.assignOrder(order)
        courierOrderRepository.save(courierOrder)


        // Notify restaurant
        // Should probably be outside of this use-case completely
        // As it's not really part of the same transaction
        // 🤔 who is responsible for posting an event?
        val restaurantOrder = restaurant.placeOrder(order)
        restaurantOrderRepository.save(restaurantOrder)

        // Clear the basket
        basketRepository.delete(basket)

        return order
    }
}