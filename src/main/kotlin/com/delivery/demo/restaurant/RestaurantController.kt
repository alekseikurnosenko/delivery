package com.delivery.demo.restaurant

import com.delivery.demo.Address
import com.delivery.demo.EventPublisher
import com.delivery.demo.JacksonConfiguration
import com.delivery.demo.order.OrderDTO
import com.delivery.demo.order.asDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.joda.money.format.MoneyFormatterBuilder
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
    private val restaurantRepository: RestaurantRepository,
    private val restaurantOrderRepository: RestaurantOrderRepository,
    private val eventPublisher: EventPublisher
) {

    @Operation(summary = "Get list of restaurants")
    @GetMapping("")
    fun restaurants(): List<RestaurantDTO> {
        return restaurantRepository.findAll().map { it.asDTO() }
    }

    @Operation(summary = "Create new restaurant")
    @PostMapping("")
    fun createRestaurant(@RequestBody @Valid input: CreateRestaurantInput): RestaurantDTO {
        val restaurant = Restaurant(
            id = UUID.randomUUID(),
            name = input.name,
            address = input.address,
            currency = CurrencyUnit.of(input.currency)
        )
        return restaurantRepository.save(restaurant).asDTO()
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

    @PostMapping("/{restaurantId}/dishes", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Create dish served by restaurant")
    @ApiResponse(responseCode = "200", description = "Successfully added a dish")
    fun createDish(
        @PathVariable("restaurantId") restaurantId: UUID,
        @Parameter(description = "Dish to add", required = true) @RequestBody @Valid dish: CreateDishInput
    ): DishDTO {
        val restaurant = restaurantRepository.findById(restaurantId)
            .orElseThrow { ResourceNotFoundException("Restaurant not found") }
        return restaurant.addDish(dish.name, Money.parse(dish.price)).asDTO()
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
        @RequestParam(required = false) status: RestaurantOrderStatus?
    ): List<RestaurantOrderDTO> {
        return if (status != null) {
            restaurantOrderRepository.findByRestaurantIdAndStatus(restaurantId, status).map { it.asDTO() }
        } else {
            restaurantOrderRepository.findByRestaurantId(restaurantId).map { it.asDTO() }
        }
    }

    @PostMapping("/{restaurantId}/orders/{orderId}/startPreparing")
    fun startPreparingOrder(
        @PathVariable("restaurantId", required = true) restaurantId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID,
        @RequestParam(required = false) status: RestaurantOrderStatus?
    ): RestaurantOrderDTO {
        val order = restaurantOrderRepository.findByRestaurantIdAndOrderId(restaurantId, orderId).orElseThrow {
            ResourceNotFoundException("Restaurant or Order not found")
        }
        order.startPreparing()

        eventPublisher.publish("Order", order.events)
        return order.asDTO()
    }

    @PostMapping("/{restaurantId}/orders/{orderId}/finishPreparing")
    fun finishPreparingOrder(
        @PathVariable("restaurantId", required = true) restaurantId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID,
        @RequestParam(required = false) status: RestaurantOrderStatus?
    ): RestaurantOrderDTO {
        val order = restaurantOrderRepository.findByRestaurantIdAndOrderId(restaurantId, orderId).orElseThrow {
            ResourceNotFoundException("Restaurant or Order not found")
        }
        order.finishPreparing()

        eventPublisher.publish("Order", order.events)
        return order.asDTO()
    }
}

data class CreateDishInput(
    val name: String,
    val price: String
)

data class CreateRestaurantInput(
    val name: String,
    val address: Address,
    val currency: String
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)


fun Restaurant.asDTO(): RestaurantDTO = RestaurantDTO(
    id.toString(),
    name
)

data class RestaurantDTO(
    val id: String,
    val name: String
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

data class DishDTO(
    val id: String,
    val name: String,
    val price: JacksonConfiguration.MoneyView
)

data class RestaurantOrderDTO(
    val status: RestaurantOrderStatus,
    val order: OrderDTO
)

fun RestaurantOrder.asDTO() = RestaurantOrderDTO(
    status = status,
    order = order.asDTO()
)