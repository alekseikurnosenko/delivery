package com.delivery.demo.outbox

import com.delivery.demo.DomainEvent
import com.delivery.demo.EventPublisher
import com.delivery.demo.order.OrderPaid
import com.delivery.demo.order.OrderPlaced
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import javax.transaction.Transactional

/**
 * Event publisher using transactional outbox pattern.
 * Relies on Spring's Scheduler to periodically poll the database
 */
@Service
class OutboxEventPublisher(
    private val outboxRepository: OutboxRepository,
    private val rabbitMQPublisher: RabbitMQPublisher,
    private val messageConverter: MessageConverter
) : EventPublisher {

    @Transactional
    override fun publish(events: List<DomainEvent>, topic: String) {
        outboxRepository.saveAll(events.map {
            val routingKey = when (it) {
                is OrderPlaced -> "order.placed"
                is OrderPaid -> "order.paid"
                else -> it.routingKey
            }
            val message = messageConverter.toMessage(it, MessageProperties())
            OutboxMessage(
                message = message,
                routingKey = routingKey
            )
        })
    }

    @Scheduled(fixedRate = 500L)
    fun runJob() {
        // Due to Spring magic, we cannot have both @Scheduled and @Transactional at the same time
        // This is a work around
        rabbitMQPublisher.publish()
    }
}

@Service
class RabbitMQPublisher(
    private val outboxRepository: OutboxRepository,
    private val template: RabbitTemplate,
    private val exchange: TopicExchange
) {

    @Transactional
    fun publish() {
        val messages = outboxRepository.findAll()
        messages.forEach {
            template.send(exchange.name, it.routingKey, it.message)
        }
        outboxRepository.deleteAll(messages)
    }
}