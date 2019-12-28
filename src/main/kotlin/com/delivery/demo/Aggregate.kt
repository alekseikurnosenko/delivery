package com.delivery.demo

import javax.persistence.Transient

abstract class Aggregate {
    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()


    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    open val events: List<DomainEvent> = domainEvents
}