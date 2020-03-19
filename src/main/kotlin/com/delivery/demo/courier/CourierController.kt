package com.delivery.demo.courier

import com.auth0.spring.security.api.authentication.AuthenticationJsonWebToken
import com.delivery.demo.Address
import com.delivery.demo.EventPublisher
import com.delivery.demo.OptimisticRetryable
import com.delivery.demo.delivery.DeliveryRequestDTO
import com.delivery.demo.delivery.asDTO
import com.delivery.demo.order.OrderDTO
import com.delivery.demo.order.OrderRepository
import com.delivery.demo.order.UnknownOrderException
import com.delivery.demo.order.asDTO
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@CrossOrigin
@RequestMapping(
    "/api/couriers",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "couriers")
class CourierController(
    val courierRepository: CourierRepository,
    val eventPublisher: EventPublisher,
    val courierLocationRepository: CourierLocationRepository,
    val orderRepository: OrderRepository
) {

    @OptimisticRetryable
    @Transactional
    @PostMapping("")
    fun createCourier(
        @RequestBody input: CreateCourierInput,
        token: AuthenticationJsonWebToken
    ): CourierDTO {
        val courier = Courier.new(
            accountId = token.name,
            fullName = input.name
        )
        eventPublisher.publish(courier.events)
        return courierRepository.save(courier).asDTO(withOrders = true)
    }

    @GetMapping("")
    fun couriers(): List<CourierDTO> {
        return courierRepository.findAll().map { it.asDTO(withOrders = true) }
    }

    @GetMapping("/me")
    fun ownCourier(token: AuthenticationJsonWebToken): CourierDTO? {
        return courierRepository.findByUserId(token.name).map { it.asDTO(withOrders = true) }.orElse(null)
    }

    @GetMapping("/{courierId}/orders")
    fun orders(
        @PathVariable("courierId", required = true) courierId: UUID
    ): List<OrderDTO> {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }
        return courier.activeOrders.map { it.asDTO() }
    }

    @GetMapping("/{courierId}/requests")
    fun pendingDeliveryRequests(
        @PathVariable("courierId", required = true) courierId: UUID
    ): List<DeliveryRequestDTO> {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }

        return courier.pendingDeliveryRequests.map { it.asDTO() }
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/requests/{orderId}/accept")
    fun acceptDeliveryRequest(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }
        val order = orderRepository.findById(orderId)
            .orElseThrow { UnknownOrderException(orderId) }

        order.acceptDeliveryRequest(courier)
        courier.onDeliveryRequestAccepted(order)

        eventPublisher.publish(order.events)
        return order.asDTO()
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/requests/{orderId}/reject")
    fun rejectDeliveryRequest(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }
        val order = orderRepository.findById(orderId)
            .orElseThrow { UnknownOrderException(orderId) }

        order.rejectDeliveryRequest(courier)
        courier.onDeliveryRequestRejected(order)

        eventPublisher.publish(order.events)
        return order.asDTO()
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/orders/{orderId}/confirmPickup")
    fun confirmOrderPickup(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }
        val order = orderRepository.findById(orderId)
            .orElseThrow { UnknownOrderException(orderId) }

        order.confirmPickup(courier)

        eventPublisher.publish(order.events)
        return order.asDTO()
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/orders/{orderId}/confirmDropoff")
    fun confirmDropoff(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { CourierNotFoundException(courierId) }
        val order = orderRepository.findById(orderId)
            .orElseThrow { UnknownOrderException(orderId) }

        order.confirmDropoff(courier)

        eventPublisher.publish(order.events)
        return order.asDTO()
    }

    @PostMapping("/{courierId}/location")
    fun updateLocation(
        @PathVariable("courierId", required = true) courierId: UUID,
        @RequestBody input: UpdateLocationInput
    ) {
        courierLocationRepository.updateLocation(courierId, LocationReport(input.latLng, Date()))
        val event = CourierLocationUpdated(courierId, input.latLng)

        eventPublisher.publish(listOf(event))
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/startShift")
    fun startShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }

        courier.startShift()

        eventPublisher.publish(courier.events)
        return courier.asDTO(withOrders = true)
    }

    @OptimisticRetryable
    @Transactional
    @PostMapping("/{courierId}/stopShift")
    fun stopShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }

        courier.stopShift()

        eventPublisher.publish(courier.events)
        return courier.asDTO(withOrders = true)
    }

    @GetMapping("/statistics")
    fun statistics(): CourierStatistics {
        return CourierStatistics(
            registered = courierRepository.count().toInt(),
            online = courierRepository.countByOnShift(true).toInt()
        )
    }

}

data class CourierStatistics(
    val registered: Int,
    val online: Int
)

data class CreateCourierInput(
    val name: String
)

data class UpdateLocationInput(
    val latLng: LatLng
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class CourierNotFoundException(courierId: UUID) : Exception("Courier(id=$courierId) not found")