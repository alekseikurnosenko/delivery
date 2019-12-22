package com.delivery.demo

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@CrossOrigin
@Tag(name = "index")
class IndexController {

    @Operation
    @GetMapping("/api/whoami")
    fun user(): Authentication? {
        val auth = SecurityContextHolder.getContext()
            .authentication

        return auth
    }

}