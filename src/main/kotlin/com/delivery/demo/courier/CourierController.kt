package com.delivery.demo.courier

import com.delivery.demo.Address
import com.delivery.demo.order.OrderStatus
import io.swagger.v3.oas.annotations.tags.Tag
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
    val courierRepository: CourierRepository
) {

    @GetMapping("/{courierId}/orders")
    fun orders(
        @PathVariable("courierId", required = true) courierId: UUID
    ): List<CourierAssignmentDTO> {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        return courier.orders
            .filter {
                it.status != OrderStatus.Delivered
            }
            .map {
                CourierAssignmentDTO(
                    id = it.id.toString(),
                    restaurantName = it.restaurant.name,
                    pickupAddress = it.restaurant.address,
                    deliveryAddress = it.deliveryAddress
                )
            }
    }

    @Transactional
    @PostMapping("/{courierId}/location")
    fun updateLocation(
        @PathVariable("courierId", required = true) courierId: UUID,
        @RequestBody input: UpdateLocationInput
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        courier.updateLocation(LocationReport(input.latLng, Date()))
        return courier.asDTO()
    }

    @Transactional
    @PostMapping("/{courierId}/startShift")
    fun startShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        courier.startShift()
        return courier.asDTO()
    }

    @PostMapping("/{courierId}/stopShift")
    fun stopShift(
        @PathVariable("courierId", required = true) courierId: UUID
    ): CourierDTO {
        val courier = courierRepository.findById(courierId).orElseThrow { Exception("Unknown courierId: $courierId") }
        courier.stopShift()
        return courier.asDTO()
    }

}

data class CourierAssignmentDTO(
    val id: String,
    val restaurantName: String,
    val pickupAddress: Address,
    val deliveryAddress: Address
)

data class UpdateLocationInput(
    val latLng: LatLng
)
