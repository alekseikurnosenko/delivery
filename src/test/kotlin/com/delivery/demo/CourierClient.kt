package com.delivery.demo

import com.delivery.demo.courier.CourierDTO
import com.delivery.demo.courier.CreateCourierInput
import com.delivery.demo.delivery.DeliveryRequested
import com.delivery.demo.notification.WebSocketMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity
import org.springframework.web.client.postForObject
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.concurrent.ConcurrentLinkedDeque

@Component
class CourierClientFactory(
        private val auth0Client: Auth0Client,
        private val objectMapper: ObjectMapper
) {
    fun create(
            name: String,
            endpoint: String,
            serverPort: Int
    ): CourierClient {
        return CourierClient(name, endpoint, serverPort, auth0Client, objectMapper)
    }
}

class CourierClient(
        private val name: String,
        private val endpoint: String,
        private val serverPort: Int,
        private val auth0Client: Auth0Client,
        private val objectMapper: ObjectMapper
) {

    private var courier: CourierDTO
    private val restTemplate = RestTemplate()

    private lateinit var token: String
    val events = ConcurrentLinkedDeque<Any>()

    init {
        val email = "$name@delivery.com"
        try {
            auth0Client.createUser(email, "password")
        } catch (e: Throwable) {

        }
        token = auth0Client.getUserToken(email, "password")

        restTemplate.interceptors = listOf(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.add("Authorization", "Bearer $token")
                    return@ClientHttpRequestInterceptor execution.execute(request, body)
                }
        )

        val input = CreateCourierInput(name)
        courier = restTemplate.postForObject<CourierDTO>(api("/couriers"), input)

        initWebSocket()
    }

    private fun initWebSocket() {
        val client = StandardWebSocketClient()
        val session = client.doHandshake(object : TextWebSocketHandler() {
            override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                val socketMessage = objectMapper.readValue(message.payload, WebSocketMessage::class.java)
                val event = objectMapper.readValue(socketMessage.payload, Class.forName(socketMessage.type))
                events.add(event)
            }
        }, WebSocketHttpHeaders(), URI.create("ws://$endpoint:$serverPort/ws")).get()

        session.sendMessage(TextMessage("X-Authorization: $token"))
    }

    val id: String
        get() = courier.id

    override fun toString(): String {
        return courier.id
    }

    private fun api(path: String) = "http://$endpoint:$serverPort$path"

    fun acceptCurrentDeliveryRequest() {
        val deliveryRequest = events.filterIsInstance(DeliveryRequested::class.java).last()
        restTemplate.postForEntity<Unit>(api("/couriers/$id/requests/${deliveryRequest.orderId}/accept"))
    }
}