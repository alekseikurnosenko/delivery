package com.delivery.demo

import com.delivery.demo.restaurant.OrderPreparationFinished
import com.delivery.demo.restaurant.OrderPreparationStarted
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
        eventSubscriber.subscribe(OrderPreparationStarted::class.java, "Order") { event ->
            send(event)
        }
        eventSubscriber.subscribe(OrderPreparationFinished::class.java, "Order") { event ->
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