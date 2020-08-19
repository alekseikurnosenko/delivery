package com.delivery.demo.restaurant

import com.auth0.spring.security.api.authentication.AuthenticationJsonWebToken
import com.delivery.demo.Address
import com.delivery.demo.EventPublisher
import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.order.OrderDTO
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.OrderStatus
import com.delivery.demo.order.asDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.joda.money.format.MoneyFormatterBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.validation.Valid

@RestController
@CrossOrigin
@RequestMapping(
        "/restaurants",
        produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "restaurants", description = "Manage restaurants")
class RestaurantController(
        private val restaurantRepository: RestaurantRepository,
        private val orderRepository: OrderRepository,
        private val eventPublisher: EventPublisher
) {

    @Operation(summary = "Get list of restaurants")
    @GetMapping("")
    fun restaurants(): List<RestaurantDTO> {
        return restaurantRepository.findAll().map { it.asDTO() }
    }

    @Operation(summary = "Create new restaurant")
    @PostMapping("")
    fun createRestaurant(
            @RequestBody @Valid input: CreateRestaurantInput,
            token: AuthenticationJsonWebToken
    ): RestaurantDTO {
        val restaurant = Restaurant.new(
                accountId = token.name,
                name = input.name,
                address = input.address,
                currency = CurrencyUnit.of(input.currency),
                imageUrl = input.imageUrl
        )

        eventPublisher.publish(restaurant.events)
        return restaurantRepository.save(restaurant).asDTO()
    }

    @GetMapping("/me")
    fun ownRestaurant(token: AuthenticationJsonWebToken): RestaurantDTO? {
        return restaurantRepository.findByAccountId(token.name).map { it.asDTO() }.orElse(null)
    }

    @Operation(summary = "Get restaurant info")
    @GetMapping("/{restaurantId}")
    fun restaurant(
            @PathVariable("restaurantId", required = true) restaurantId: UUID
    ): RestaurantDTO {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }
                .asDTO()
    }

    @Operation(summary = "Edit restaurant info")
    @PutMapping("/{restaurantId}")
    @Transactional
    fun editRestaurant(
            @PathVariable("restaurantId", required = true) restaurantId: UUID,
            @RequestBody @Valid input: EditRestaurantInput
    ): RestaurantDTO {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }

        restaurant.update(input.name, input.address, input.imageUrl)
        return restaurantRepository.save(restaurant).asDTO()
    }

    @DeleteMapping("/{restaurantId}")
    @Transactional
    fun deleteRestaurant(
            @PathVariable("restaurantId", required = true) restaurantId: UUID
    ) {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }

        restaurant.delete()
    }

    @PostMapping("/{restaurantId}/dishes", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Create dish served by restaurant")
    @ApiResponse(responseCode = "200", description = "Successfully added a dish")
    @Transactional
    fun createDish(
            @PathVariable("restaurantId") restaurantId: UUID,
            @Parameter(description = "Dish to add", required = true) @RequestBody @Valid input: CreateDishInput
    ): DishDTO {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        return restaurant.addDish(input.name, Money.of(restaurant.currency, input.price), input.imageUrl).asDTO()
    }

    @PutMapping("/{restaurantId}/dishes/{dishId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun editDish(
            @PathVariable("restaurantId") restaurantId: UUID,
            @PathVariable("dishId") dishId: UUID,
            @RequestBody @Valid input: EditDishInput
    ): DishDTO {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }

        val dish = restaurant.editDish(dishId, input.name, Money.of(restaurant.currency, input.price), input.imageUrl)

        return dish.asDTO()
    }

    @DeleteMapping("/{restaurantId}/dishes/{dishId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Transactional
    fun deleteDish(
            @PathVariable("restaurantId") restaurantId: UUID,
            @PathVariable("dishId") dishId: UUID
    ) {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }

        restaurant.deleteDish(dishId)
    }

    @Operation(summary = "Get restaurant dishes")
    @GetMapping("/{restaurantId}/dishes")
    fun restaurantDishes(
            @PathVariable("restaurantId", required = true) restaurantId: UUID
    ): List<DishDTO> {
        val restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        return restaurant.dishes.map { it.asDTO() }
    }

    @GetMapping("/{restaurantId}/orders")
    fun orders(
            @PathVariable("restaurantId", required = true) restaurantId: UUID,
            @RequestParam(required = false) status: OrderStatus?
    ): List<OrderDTO> {
        return if (status != null) {
            orderRepository.findByStatusAndRestaurantId(status, restaurantId).map { it.asDTO() }
        } else {
            orderRepository.findByRestaurantId(restaurantId).map { it.asDTO() }
        }
    }

    @Transactional
    @PostMapping("/{restaurantId}/orders/{orderId}/startPreparing")
    fun startPreparingOrder(
            @PathVariable("restaurantId", required = true) restaurantId: UUID,
            @PathVariable("orderId", required = true) orderId: UUID,
            @RequestParam(required = false) status: OrderStatus?
    ): OrderDTO {
        val order = orderRepository.findById(orderId).orElseThrow {
            ResourceNotFoundException("Order(id=$orderId) not found")
        }
        if (order.restaurant.id != restaurantId) {
            throw Exception("Order(id=$orderId) doesn't belong to the restaurant")
        }
        order.startPreparing()

        eventPublisher.publish(order.events)
        return order.asDTO()
    }

    @Transactional
    @PostMapping("/{restaurantId}/orders/{orderId}/finishPreparing")
    fun finishPreparingOrder(
            @PathVariable("restaurantId", required = true) restaurantId: UUID,
            @PathVariable("orderId", required = true) orderId: UUID,
            @RequestParam(required = false) status: OrderStatus?
    ): OrderDTO {
        val order = orderRepository.findById(orderId).orElseThrow {
            ResourceNotFoundException("Order(id=$orderId) not found")
        }
        if (order.restaurant.id != restaurantId) {
            throw Exception("Order(id=$orderId) doesn't belong to the restaurant")
        }
        order.finishPreparing()

        eventPublisher.publish(order.events)
        return order.asDTO()
    }
}

data class CreateDishInput(
        val name: String,
        val price: Double,
        val imageUrl: String?
)

data class EditDishInput(
        val name: String,
        val price: Double,
        val imageUrl: String?
)

data class CreateRestaurantInput(
        val name: String,
        val address: Address,
        val currency: String,
        val imageUrl: String?
)

data class EditRestaurantInput(
        val name: String,
        val address: Address,
        val imageUrl: String?
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)


fun Restaurant.asDTO(): RestaurantDTO = RestaurantDTO(
        id = id.toString(),
        name = name,
        address = address,
        currency = currency.code
)

@Schema(name = "Restaurant")
data class RestaurantDTO(
        val id: String,
        val name: String,
        val address: Address,
        val currency: String
)

fun Dish.asDTO(): DishDTO = DishDTO(
        id = id.toString(),
        name = name,
        price = price.asDTO()
)

fun Money.asDTO(): JacksonConfiguration.MoneyView {
    val formatter = MoneyFormatterBuilder().appendCurrencySymbolLocalized().appendAmountLocalized().toFormatter()
    return JacksonConfiguration.MoneyView(
            amount = amount.toDouble(),
            currencyCode = currencyUnit.code,
            formatted = formatter.print(this)
    )
}

@Schema(name = "Dish")
data class DishDTO(
        val id: String,
        val name: String,
        val price: JacksonConfiguration.MoneyView
)