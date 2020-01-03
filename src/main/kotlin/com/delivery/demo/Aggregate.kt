package com.delivery.demo

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass
import javax.persistence.Transient

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class Aggregate {

    @CreatedDate
    @Column(updatable = false)
    var createdDate: Date? = null

    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()


    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    open val events: List<DomainEvent>
        get() = domainEvents
}