package com.delivery.demo.restaurant

import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import io.swagger.annotations.*
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@CrossOrigin
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
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        restaurant.dishes.add(Dish(null, dish.name, restaurant))
        val updateRestaurant = restaurantRepository.save(restaurant)
        return updateRestaurant.dishes.last()
    }

    @ApiOperation("Get restaurant dishes")
    @GetMapping("/{restaurantId}/dishes")
    fun restaurantDishes(
        @PathVariable("restaurantId", required = true) restaurantId: UUID
    ): List<Dish> {
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        return restaurant.dishes
    }
}

data class CreateDishInput(
    val name: String
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)