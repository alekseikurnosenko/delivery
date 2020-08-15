package com.delivery.demo.profile

import com.delivery.demo.Address
import io.swagger.v3.oas.annotations.tags.Tag
import org.hibernate.annotations.common.util.impl.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@CrossOrigin
@RequestMapping(
    "/api/profile",
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Tag(name = "profile", description = "Current profile")
class ProfileController(
    private val profileRepostiory: ProfileRepostiory
) {
    private val logger = LoggerFactory.logger(ProfileController::class.java)

    @PostMapping("/address")
    fun setAddress(@RequestBody address: Address, principal: Principal): Profile {
        return profileRepostiory.findByUserId(principal.name)
            .map { profile ->
                profile.deliveryAddress = address
                profileRepostiory.save(profile)
            }
            .orElseGet {
                // TODO: create as aggregate instead
                val newProfile = Profile()
                newProfile.userId = principal.name
                newProfile.deliveryAddress = address
                profileRepostiory.save(newProfile)
            }
    }

    @PostMapping("/payment_method")
    fun setPaymentMethod(@RequestBody input: SetPaymentMethodInput, principal: Principal): Profile {
        return profileRepostiory.findByUserId(principal.name)
            .map { profile ->
                profile.paymentMethodId = input.paymentMethodId
                profileRepostiory.save(profile)
            }
            .orElseThrow {
                ProfileNotFoundException(principal.name)
            }
    }

    data class SetPaymentMethodInput(val paymentMethodId: String)
}

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ProfileNotFoundException(val userId: String) : Exception("Profile(userId=$userId) not found")