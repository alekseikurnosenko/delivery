package com.delivery.demo

import com.delivery.demo.courier.CourierAdded
import com.delivery.demo.courier.CourierLocationUpdated
import com.delivery.demo.order.*
import com.delivery.demo.restaurant.RestaurantAdded
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.common.util.impl.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

data class WebSocketMessage(val type: String, val payload: String)

@Component
class SocketHandler(
    eventSubscriber: EventSubscriber,
    val objectMapper: ObjectMapper,
    @Qualifier("publishableEvents") events: List<Class<out DomainEvent>>
) : TextWebSocketHandler() {

    private val logger = LoggerFactory.logger(SocketHandler::class.java)

    private val sessions = ConcurrentLinkedDeque<WebSocketSession>()

    data class Ids(
        var userId: String,
        var orderId: UUID,
        var restaurantId: UUID,
        var courierId: UUID? = null
    )

    private val orders = mutableListOf<Ids>()
    private val restaurantsIds = mutableMapOf<UUID, String>()
    private val courierIds = mutableMapOf<UUID, String>()

    init {
        eventSubscriber.subscribeAll(
            events
        ) { event ->

            // Retrieving ids and updating the lists have to happen at one go, since we might remove something from the list
            // But still send it

            // Am I supposed to write rules for every event? omg
            // Moreover, this "map" would need to re-hydrated on server start
            // even if I allow users to subscribe to some particular ids
            // would be semi-useless since we would anyway need to verify that we can actually access them
            val affectedUserIds: List<String> = when (event) {
                is CourierLocationUpdated -> {
                    // Users
                    orders.filter { it.courierId == event.courierId }.map { it.userId }
                    // MB: restaurants?
                }
                is OrderPreparationStarted -> {
                    // admins?
                    listOf()
                }
                is OrderPreparationFinished -> {
                    val couriers = orders.filter { it.orderId == event.orderId }.mapNotNull { courierIds[it.courierId] }
                    users(event.orderId) + couriers
                }
                is OrderPlaced -> {
                    orders.add(Ids(event.userId, event.orderId, event.restaurantId))

                    restaurants(event.restaurantId) + users(event.orderId)
                }
                is OrderAssigned -> {
                    orders.find { it.orderId == event.orderId }?.let { it.courierId = event.courierId }

                    users(event.orderId) + couriers(event.courierId)
                }
                is OrderPickedUp -> {
                    users(event.orderId)
                }
                is OrderDelivered -> {
                    val session = orders.find { it.orderId == event.orderId }
                    orders.removeAll { it.orderId == event.orderId }

                    listOfNotNull(session?.userId)
                }
                is CourierAdded -> {
                    courierIds[event.courierId] = event.accountId

                    listOf() // Admin
                }
                is RestaurantAdded -> {
                    restaurantsIds[event.restaurantId] = event.accountId

                    listOf()
                }
                else -> listOf()
            }

            if (event::class != CourierLocationUpdated::class) {
                println("sending $event")
            }
            val type = event::class.java.name
            val payload = objectMapper.writeValueAsString(event)
            sessions.filter { session ->
                session.principal?.let { affectedUserIds.contains(it.name) } ?: true
            }
                .forEach { session ->
                    // We don't allow un-authenticated users
                    // LOLTOMCAT: https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
                    // Not sure whether we still need btw
                    synchronized(session) {
                        try {
                            session.sendMessage(
                                TextMessage(objectMapper.writeValueAsString(WebSocketMessage(type, payload)))
                            )
                        } catch (e: Exception) {
                            logger.error("Failed to send message", e)
                        }
                    }
                }
        }

    }

    fun users(orderId: UUID) = orders.filter { it.orderId == orderId }.map { it.userId }
    fun couriers(courierId: UUID) = listOfNotNull(courierIds[courierId])
    fun restaurants(restaurantId: UUID) = listOfNotNull(restaurantsIds[restaurantId])

    @Scheduled(fixedRate = 25 * 1000)
    fun sendHeartbeat() {
        sessions.forEach { session ->
            // LOLTOMCAT: https://bz.apache.org/bugzilla/show_bug.cgi?id=56026
            synchronized(session) {
                try {
                    session.sendMessage(PingMessage())
                } catch (e: Exception) {
                    logger.error("Failed to send ping", e)
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
                    logger.error("Failed to send message", e)
                }
            }
        }
    }

    override fun afterConnectionEstablished(session: WebSocketSession) {
//        val principal = session.principal
//        if (principal == null) {
//            session.close()
//        } else {
//            sessionIds.add(Ids(principal.name))
            sessions.add(session)
//        }

    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
//        session.principal?.let { principal -> sessionIds.removeAll { it.userId == principal.name } }
        sessions.remove(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Message from $session: $message")
    }

    override fun handlePongMessage(session: WebSocketSession, message: PongMessage) {
        super.handlePongMessage(session, message)
    }
}