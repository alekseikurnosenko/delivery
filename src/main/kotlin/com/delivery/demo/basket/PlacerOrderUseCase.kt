package com.delivery.demo.basket

import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.order.Order
import com.delivery.demo.order.OrderItem
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.Address
import org.springframework.stereotype.Service

@Service
class PlacerOrderUseCase(
    val courierRepository: CourierRepository,
    val restaurantRepository: RestaurantRepository,
    val orderRepository: OrderRepository
) {

    fun place(basket: Basket, deliveryAddress: Address): Order {
        // List all couriers that are on shift
        val couriers = courierRepository.findByOnShift(true)
        // Ensure that there are some available
        if (couriers.isEmpty()) {
            throw Exception("No courier available")
        }

//        // find the restaurant we want to order from
//        val restaurant = restaurantRepository.findById(restaurantId)
//            .orElseThrow { Exception("Unknown restaurant with id $restaurantId") }
        // Ensure it's still working
        if (!basket.restaurant.isAcceptingOrders) {
            throw Exception("Restaurant is not accepting orders")
        }
        // TODO: Charge payment

        // Find the closest courier to the resturant
        val courier = couriers.first()

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
        basket.restaurant.placeOrder(order)

        return order
    }
}