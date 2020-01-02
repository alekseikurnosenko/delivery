package com.delivery.demo

import com.delivery.demo.courier.CourierLocationUpdated
import com.delivery.demo.order.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentLinkedDeque

data class WebSocketMessage(val type: String, val payload: String)

@Component
class SocketHandler(
    eventSubscriber: EventSubscriber,
    val objectMapper: ObjectMapper
) : TextWebSocketHandler() {

    init {
        eventSubscriber.subscribeAll(
            listOf(
                CourierLocationUpdated::class.java
            ),
            "Courier"
        ) { event ->
            println("sending $event")
            send(event)
        }
        eventSubscriber.subscribeAll(
            listOf(
                OrderAssigned::class.java,
                OrderPlaced::class.java,
                OrderPreparationStarted::class.java,
                OrderPreparationFinished::class.java,
                OrderPickedUp::class.java,
                OrderDelivered::class.java
            ),
            "Order"
        ) { event ->
            println("sending $event")
            send(event)
        }
    }

    private fun send(message: Any) {
        val type = message::class.java.name
        val payload = objectMapper.writeValueAsString(message)
        sessions.forEach { session ->
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(WebSocketMessage(type, payload))))
        }
    }

    private val sessions = ConcurrentLinkedDeque<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Message from $session: $message")
    }
}