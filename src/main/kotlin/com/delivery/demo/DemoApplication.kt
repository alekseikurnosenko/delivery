package com.delivery.demo

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.demo.restaurant.model.Address
import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import java.util.*

@SpringBootApplication
class DemoApplication(
    val restaurantRepository: RestaurantRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val restaurant = Restaurant(
            id = UUID.fromString("00cf5f40-1c6c-4eab-8efc-41fb8a337a22"),
            name = "Test restaurant",
            address = Address("address", "city", "country")
        )
        restaurant.dishes.add(Dish(name = "Dish 1", restaurant = restaurant))

        restaurantRepository.save(restaurant)
    }

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}