package com.delivery.demo.restaurant

import com.delivery.demo.restaurant.model.Restaurant
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/api")
class RestaurantController(
    private val restaurantRepository: RestaurantRepository
) {

    @GetMapping("/restaurants")
    fun restaurants(): List<Restaurant> {
        return restaurantRepository.findAll()
    }

    @PostMapping("/restaurants")
    fun createRestaurant(@RequestBody @Valid restaurant: Restaurant): Restaurant {
        return restaurantRepository.save(restaurant)
    }
}