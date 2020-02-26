package com.delivery.demo.courier

import com.auth0.spring.security.api.authentication.AuthenticationJsonWebToken
import com.delivery.demo.Address
import com.delivery.demo.EventPublisher
import com.delivery.demo.delivery.DeliveryDTO
import com.delivery.demo.delivery.DeliveryRepository
import com.delivery.demo.delivery.DeliveryRequestDTO
import com.delivery.demo.delivery.asDTO
import com.delivery.demo.order.OrderDTO
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
    val deliveryRepository: DeliveryRepository
) {

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

    @Transactional
    @PostMapping("/{courierId}/requests/{deliveryId}/accept")
    fun acceptDeliveryRequest(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("deliveryId", required = true) deliveryId: UUID
    ) {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { Exception("Unknown courierId: $courierId") }
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { Exception("Unknown Delivery(id=$deliveryId") }

        delivery.acceptRequestAsCourier(courier)
        courier.acceptRequest(delivery)

        eventPublisher.publish(delivery.events)
    }

    @Transactional
    @PostMapping("/{courierId}/requests/{deliveryId}/reject")
    fun rejectDeliveryRequest(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("deliveryId", required = true) deliveryId: UUID
    ) {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { Exception("Unknown courierId: $courierId") }
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { Exception("Unknown Delivery(id=$deliveryId") }

        delivery.rejectRequestAsCourier(courier)
        courier.rejectRequest(delivery)

        eventPublisher.publish(delivery.events)
    }

    @Transactional
    @PostMapping("/{courierId}/orders/{orderId}/confirmPickup")
    fun confirmPickup(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { Exception("Unknown courierId: $courierId") }

        val order = courier.confirmOrderPickup(orderId)
        eventPublisher.publish(order.events)

        return order.asDTO()
    }

    @Transactional
    @PostMapping("/{courierId}/deliveries/{deliveryId}/confirmPickup")
    fun confirmDeliveryPickup(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("deliveryId", required = true) deliveryId: UUID
    ): DeliveryDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { Exception("Unknown courierId: $courierId") }
        val delivery = deliveryRepository.findById(deliveryId)
            .orElseThrow { Exception("Unknown Delivery(id=$deliveryId") }

        delivery.confirmPickupAsCourier(courier)

        eventPublisher.publish(delivery.events)

        return delivery.asDTO()
    }

    @Transactional
    @PostMapping("/{courierId}/orders/{orderId}/confirmDropoff")
    fun confirmDropoff(
        @PathVariable("courierId", required = true) courierId: UUID,
        @PathVariable("orderId", required = true) orderId: UUID
    ): OrderDTO {
        val courier = courierRepository.findById(courierId)
            .orElseThrow { Exception("Unknown courierId: $courierId") }

        val order = courier.confirmOrderDropoff(orderId)
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

    @PostMapping("/{courierId}/startShift")
    @Transactional
    fun startShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        courier.startShift()
        eventPublisher.publish(courier.events)
        return courierRepository.save(courier).asDTO(withOrders = true)
    }

    @PostMapping("/{courierId}/stopShift")
    @Transactional
    fun stopShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        courier.stopShift()
        eventPublisher.publish(courier.events)
        return courierRepository.save(courier).asDTO(withOrders = true)
    }

}

data class CreateCourierInput(
    val name: String
)

data class UpdateLocationInput(
    val latLng: LatLng
)

//fun CourierOrder.asDTO(withOrders = true) = CourierOrderDTO(
//    orderId = order.id.toString(),
//    restaurantName = order.restaurant.name,
//    pickupAddress = order.restaurant.address,
//    deliveryAddress = order.deliveryAddress
//)

data class CourierOrderDTO(
    val orderId: String,
    val restaurantName: String,
    val pickupAddress: Address,
    val deliveryAddress: Address
)

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class CourierNotFoundException(courierId: UUID) : Exception("Courier(id=$courierId) not found")