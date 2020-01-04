package com.delivery.demo

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentLinkedDeque

data class WebSocketMessage(val type: String, val payload: String)

@Component
class SocketHandler(
    eventSubscriber: EventSubscriber,
    val objectMapper: ObjectMapper,
    @Qualifier("publishableEvents") events: List<Class<out DomainEvent>>
) : TextWebSocketHandler() {

    init {
        eventSubscriber.subscribeAll(
            events
        ) { event ->
            println("sending $event")
            send(event)
        }

    }

    @Scheduled(fixedRate = 25 * 1000)
    fun sendHeartbeat() {
        sessions.forEach { session ->
            // LOLTOMCAT: https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
            synchronized(session) {
                try {
                    session.sendMessage(PingMessage())
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun send(message: Any) {
        val type = message::class.java.name
        val payload = objectMapper.writeValueAsString(message)
        sessions.forEach { session ->
            // LOLTOMCAT: https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
            synchronized(session) {
                try {
                    session.sendMessage(TextMessage(objectMapper.writeValueAsString(WebSocketMessage(type, payload))))
                } catch (e: Exception) {
                    // Ignore
                }
            }
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

    override fun handlePongMessage(session: WebSocketSession, message: PongMessage) {
        super.handlePongMessage(session, message)
    }
}