package com.delivery.demo

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class IndexController {

    @RequestMapping("/api/whoami")
    fun user(): Authentication? {
        val auth = SecurityContextHolder.getContext()
            .authentication

        return auth
    }

}