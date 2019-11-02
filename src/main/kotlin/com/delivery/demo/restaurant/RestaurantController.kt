package com.delivery.demo.restaurant

import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import io.swagger.annotations.*
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@RequestMapping("/api/restaurants")
@Api(value = "Restaurants", description = "REST API for Restaurants", tags = ["Restaurants"])
class RestaurantController(
    private val restaurantRepository: RestaurantRepository
) {

    @ApiOperation("Get list of restaurants")
    @GetMapping("/")
    fun restaurants(): List<Restaurant> {
        return restaurantRepository.findAll()
    }

    @ApiOperation("Create new restaurant")
    @PostMapping("/")
    fun createRestaurant(@RequestBody @Valid restaurant: Restaurant): Restaurant {
        return restaurantRepository.save(restaurant)
    }

    @ApiResponses(*[ApiResponse(code = 200, message = "Successfully added a dish")])
    @ApiOperation("Create dish served by restaurant")
    @PostMapping("/{restaurantId}/dishes")
    fun createDish(
        @PathVariable("restaurantId") restaurantId: UUID,
        @ApiParam("Dish to add", required = true) @RequestBody @Valid dish: CreateDishInput
    ): Dish {
        val restaurant = restaurantRepository.findById(restaurantId).orElseThrow { Throwable("Restaurant not found") }
        restaurant.dishes.add(Dish(null, dish.name, restaurant))
        val updateRestaurant = restaurantRepository.save(restaurant)
        return updateRestaurant.dishes.last()
    }
}

data class CreateDishInput(
    val name: String
)