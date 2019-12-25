package com.delivery.demo.basket

import com.delivery.demo.Address
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderItem
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.restaurant.RestaurantOrderRepository
import org.springframework.stereotype.Service

@Service
class PlacerOrderUseCase(
    val courierRepository: CourierRepository,
    val restaurantOrderRepository: RestaurantOrderRepository,
    val orderRepository: OrderRepository
) {

    fun place(basket: Basket, deliveryAddress: Address): Order {
        // Ensure it's still working
        if (!basket.restaurant.isAcceptingOrders) {
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
            .minBy { it.location!!.latLng.distanceTo(basket.restaurant.address.location) }
            ?: throw Exception("No courier available")

        // TODO: Charge payment

        // Create order
        val order = Order.place(
            restaurant = basket.restaurant,
            deliveryAddress = deliveryAddress,
            items = mutableListOf()
        )
        order.items.addAll(basket.items.map {
            OrderItem(
                dish = it.dish,
                quantity = it.quantity,
                order = order
            )
        })
        // Set courier
        order.assignToCourier(courier)
        orderRepository.save(order)

        // Assign
        courier.addOrder(order)
        // Notify restaurant

        // ???
        // Should probably be outside of this use-case completely
        // As it's not really part of the same transaction
        // 🤔 who is responsible for posting an event?
        val restaurantOrder = basket.restaurant.placeOrder(order)
        restaurantOrderRepository.save(restaurantOrder)

        return order
    }
}