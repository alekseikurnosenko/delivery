package com.delivery.demo.outbox

import org.springframework.amqp.core.Message
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "outbox")
class OutboxMessage(
    @Id val id: UUID = UUID.randomUUID(),
    val message: Message,
    val routingKey: String
) {
}