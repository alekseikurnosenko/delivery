package com.delivery.demo

import com.auth0.spring.security.api.JwtWebSecurityConfigurer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class SecurityConfig : WebSecurityConfigurerAdapter() {
    @Value(value = "\${auth0.audience}")
    private val apiAudience: String? = null
    @Value(value = "\${auth0.issuer}")
    private val issuer: String? = null

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        JwtWebSecurityConfigurer
            .forRS256(apiAudience, issuer)
            .configure(http)
            .cors().and().csrf().disable().authorizeRequests()
            .antMatchers("/api/**").authenticated()
    }
}