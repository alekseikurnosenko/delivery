package com.delivery.demo.profile

import com.delivery.demo.Address
import io.swagger.v3.oas.annotations.tags.Tag
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

    @PostMapping("/address")
    fun setAddress(@RequestBody address: Address, principal: Principal): Profile {
        val profile = profileRepostiory.findByName(principal.name)
            .map { profile ->
                profile.deliveryAddress = address
                profileRepostiory.save(profile)
            }
            .orElseGet {
                val newProfile = Profile()
                newProfile.name = principal.name
                newProfile.deliveryAddress = address
                profileRepostiory.save(newProfile)
            }

        return profile
    }
}