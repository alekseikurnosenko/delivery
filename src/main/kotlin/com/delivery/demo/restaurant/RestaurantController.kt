package com.delivery.restaurant

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.model.Dish
import com.delivery.restaurant.model.Restaurant
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.money.Money
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@CrossOrigin
@RequestMapping(
    "/api/restaurants",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "restaurants", description = "Manage restaurants")
class RestaurantController(
    private val restaurantRepository: RestaurantRepository
) {

    @Operation(summary = "Get list of restaurants")
    @GetMapping("/")
    fun restaurants(): List<Restaurant> {
        return restaurantRepository.findAll()
    }

    @Operation(summary = "Create new restaurant")
    @PostMapping("/")
    fun createRestaurant(@RequestBody @Valid input: CreateRestaurantInput): Restaurant {
        val restaurant = Restaurant(
            id = UUID.randomUUID(),
            name = input.name,
            address = input.address
        )
        return restaurantRepository.save(restaurant)
    }

    @Operation(summary = "Get restaurant info")
    @GetMapping("/{restaurantId}")
    fun restaurant(
        @PathVariable("restaurantId", required = true) restaurantId: UUID
    ): Restaurant {
        return restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("Restaurant not found") }
    }

    @PostMapping("/{restaurantId}/dishes", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Create dish served by restaurant")
    @ApiResponse(responseCode = "200", description = "Successfully added a dish")
    fun createDish(
        @PathVariable("restaurantId") restaurantId: UUID,
        @Parameter(description = "Dish to add", required = true) @RequestBody @Valid dish: CreateDishInput
    ): Dish {
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        val dish = restaurant.addDish(dish.name, Money.parse(dish.price))
        return dish
    }

    @Operation(summary = "Get restaurant dishes")
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
    val name: String,
    val price: String
)

data class CreateRestaurantInput(
    val name: String,
    val address: Address
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)