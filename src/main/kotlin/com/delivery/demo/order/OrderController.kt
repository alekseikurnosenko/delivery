package com.delivery.demo.order

import com.delivery.demo.restaurant.ResourceNotFoundException
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin
@RequestMapping(
    "/api/orders",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "orders")
class OrderController(
    private val orderRepository: OrderRepository
) {

    @GetMapping("")
    fun orders(): List<OrderDTO> {
        return orderRepository.findAllByOrderByCreatedDateDesc().map { it.asDTO() }
    }

    @GetMapping("/{orderId}")
    fun order(
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val order = orderRepository.findById(orderId).orElseThrow { ResourceNotFoundException("Order not found") }
        return order.asDTO()
    }
}

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class UnknownOrderException(orderId: UUID) : Exception("Unknown Order(id=$orderId)")