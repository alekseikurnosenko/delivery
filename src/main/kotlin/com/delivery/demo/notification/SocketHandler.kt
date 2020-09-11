package com.delivery.demo.notification

import com.auth0.spring.security.api.authentication.PreAuthenticatedAuthenticationJsonWebToken
import com.delivery.demo.DomainEvent
import com.delivery.demo.courier.CourierAdded
import com.delivery.demo.courier.CourierLocationUpdated
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.delivery.DeliveryRequested
import com.delivery.demo.order.*
import com.delivery.demo.restaurant.RestaurantAdded
import com.delivery.demo.restaurant.RestaurantRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.annotations.common.util.impl.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.security.Principal
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque

data class WebSocketMessage(val type: String, val payload: String)

@Component
class SocketHandler(
        val objectMapper: ObjectMapper,
        courierRepository: CourierRepository,
        restaurantRepository: RestaurantRepository,
        @Qualifier("publishableEvents") events: List<Class<out DomainEvent>>,
        private val authenticationProvider: AuthenticationManager
) : TextWebSocketHandler() {

    private val orders = mutableListOf<Ids>()
    private val restaurantsIds = mutableMapOf<UUID, String>()
    private val courierIds = mutableMapOf<UUID, String>()

    init {
        val couriers = courierRepository.findAll()
        couriers.forEach {
            courierIds[it.id] = it.accountId
        }

        restaurantRepository.findAll().forEach {
            restaurantsIds[it.id] = it.accountId
        }
    }

    private val logger = LoggerFactory.logger(SocketHandler::class.java)

    private val sessions = ConcurrentLinkedDeque<WebSocketSession>()
    private val sessionPrincipals = mutableMapOf<WebSocketSession, Principal>()

    data class Ids(
            var accountId: String,
            var orderId: UUID,
            var restaurantId: UUID,
            var courierId: UUID? = null
    )


    @RabbitListener(queues = ["websocket"])
    fun onEvent(event: DomainEvent) {
        // Retrieving ids and updating the lists have to happen at one go, since we might remove something from the list
        // But still send it

        // Am I supposed to write rules for every event? omg
        // Moreover, this "map" would need to re-hydrated on server start
        // even if I allow users to subscribe to some particular ids
        // would be semi-useless since we would anyway need to verify that we can actually access them
        val affectedUserIds: List<String> = when (event) {
            is CourierLocationUpdated -> {
                // Users
                orders.filter { it.courierId == event.courierId }.map { it.accountId }
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

                users(event.orderId)
            }
            is OrderPaid -> {
                // A bit weird, but maybe it makes sense?
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

                listOfNotNull(session?.accountId)
            }
            is OrderCanceled -> {
                val session = orders.find { it.orderId == event.orderId }
                orders.removeAll { it.orderId == event.orderId }

                listOfNotNull(session?.accountId)
            }
            is CourierAdded -> {
                courierIds[event.courierId] = event.accountId

                listOf() // Admin
            }
            is RestaurantAdded -> {
                restaurantsIds[event.restaurantId] = event.accountId

                listOf()
            }
            is DeliveryRequested -> {
                couriers(event.courierId)
            }
            else -> listOf()
        }

        if (event::class != CourierLocationUpdated::class) {
            println("sending $event to $affectedUserIds")
        }
        val type = event::class.java.name
        val payload = objectMapper.writeValueAsString(event)
        sessions.filter { session ->
            sessionPrincipals[session]?.let { affectedUserIds.contains(it.name) } ?: true
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

    fun users(orderId: UUID) = orders.filter { it.orderId == orderId }.map { it.accountId }
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
        session.principal?.let { sessionPrincipals[session] = it }
//        }

    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
//        session.principal?.let { principal -> sessionIds.removeAll { it.userId == principal.name } }
        sessions.remove(session)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        println("Message from $session: $message")
        if (message.payload.startsWith("X-Authorization: ")) {
            val token = message.payload.substringAfter("X-Authorization: ")

            val preAuth = PreAuthenticatedAuthenticationJsonWebToken.usingToken(token)
            val auth = authenticationProvider.authenticate(preAuth)
            sessionPrincipals[session] = auth
        }
    }

    override fun handlePongMessage(session: WebSocketSession, message: PongMessage) {
        super.handlePongMessage(session, message)
    }
}