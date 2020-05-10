package com.delivery.demo

import com.delivery.demo.delivery.DeliveryRequestAccepted
import com.delivery.demo.delivery.DeliveryRequestRejected
import com.delivery.demo.delivery.DeliveryRequestTimedOut
import com.delivery.demo.order.OrderPlaced
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

interface Routable {
    val routingKey: String
}

@Configuration
class RabbitMQConfiguration {

    @Bean
    fun connectionFactory(): ConnectionFactory {
        val uri = System.getenv("CLOUDAMQP_URL") ?: "amqp://guest:guest@localhost"

        return CachingConnectionFactory(uri)
    }

    @Bean
    fun amqpAdmin(connectionFactory: ConnectionFactory): AmqpAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun jackson2Converter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

    @Bean
    fun exchange() = TopicExchange("main", true, false)


    @Bean
    fun eventBinding(exchange: TopicExchange): Declarables {
        val websocketQueue = Queue("websocket")
        val orderPlacedQueue = Queue(OrderPlaced.queue)
        val orderPaidQueue = Queue("order.paid")
        val deliveryRequestAcceptedQueue = Queue(DeliveryRequestAccepted.queue)
        val deliveryRequestRejectedQueue = Queue(DeliveryRequestRejected.queue)
        val deliveryRequestTimedOutQueue = Queue(DeliveryRequestTimedOut.queue)
        return Declarables(
            websocketQueue,
            orderPlacedQueue,
            orderPaidQueue,
            deliveryRequestAcceptedQueue,
            deliveryRequestRejectedQueue,
            deliveryRequestTimedOutQueue,

            BindingBuilder.bind(orderPlacedQueue).to(exchange).with("order.placed"),
            BindingBuilder.bind(orderPaidQueue).to(exchange).with("order.paid"),

            BindingBuilder.bind(deliveryRequestAcceptedQueue).to(exchange).with(DeliveryRequestAccepted.queue),
            BindingBuilder.bind(deliveryRequestRejectedQueue).to(exchange).with(DeliveryRequestRejected.queue),
            BindingBuilder.bind(deliveryRequestTimedOutQueue).to(exchange).with(DeliveryRequestTimedOut.queue),

            BindingBuilder.bind(websocketQueue).to(exchange).with("#")
        )
    }

}