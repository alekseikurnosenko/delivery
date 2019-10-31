package com.delivery.demo.restaurant

import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.*
import java.util.*
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

    @PostMapping("/restaurants/{restaurantId}/dishes")
    fun createDish(@PathVariable restaurantId: UUID, @RequestBody @Valid dish: CreateDishInput): Dish {
        val restaurant = restaurantRepository.findById(restaurantId).orElseThrow { Throwable("Restaurant not found") }
        restaurant.dishes.add(Dish(null, dish.name, restaurant))
        val updateRestaurant = restaurantRepository.save(restaurant)
        return updateRestaurant.dishes.last()
    }
}

data class CreateDishInput(
    val name: String
)