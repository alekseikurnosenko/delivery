package com.delivery.demo.order

import com.delivery.demo.Auth0Client
import com.delivery.demo.DemoApplication
import com.delivery.demo.notification.FirebaseService
import com.delivery.demo.notification.NoopFirebaseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.test.context.ContextConfiguration

@TestConfiguration
class TestConfiguration {

    @Bean
    fun firebaseService(): FirebaseService {
        return NoopFirebaseService()
    }
}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ContextConfiguration(classes = [DemoApplication::class, com.delivery.demo.order.TestConfiguration::class], loader = SpringBootContextLoader::class)
@AutoConfigureTestDatabase
internal class OrderControllerIntegrationTest {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Autowired
    lateinit var auth0Client: Auth0Client

    @BeforeEach
    fun setUp() {
        auth0Client.createUser("testuser1@delivery.com", "password")
        val token = auth0Client.getUserToken("testuser1@delivery.com", "password")

        restTemplate.restTemplate.interceptors = listOf(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.add("Authorization", "Bearer $token")
                    return@ClientHttpRequestInterceptor execution.execute(request, body)
                }
        )
    }

    @Test
    fun `fetch orders`() {
        val page = restTemplate.getForEntity<String>("/orders")

        assertThat(page.statusCode).isEqualTo(HttpStatus.OK)
    }
}