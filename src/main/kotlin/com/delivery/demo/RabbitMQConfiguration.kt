package com.delivery.demo

import com.delivery.demo.order.OrderPlaced
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class RabbitMQConfiguration {

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
        return Declarables(
            websocketQueue,
            orderPlacedQueue,
            orderPaidQueue,
            BindingBuilder.bind(orderPlacedQueue).to(exchange).with("order.placed"),
            BindingBuilder.bind(orderPaidQueue).to(exchange).with("order.paid"),
            BindingBuilder.bind(websocketQueue).to(exchange).with("#")
        )
    }

}