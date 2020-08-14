package com.delivery.demo

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.util.ProxyUtils
import java.util.*
import javax.persistence.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AbstractEntity {

    @Id
    val id: UUID = UUID.randomUUID()

    @CreatedDate
    @Column(updatable = false)
    var createdAt: Date? = null

    @Transient
    private val domainEvents = mutableListOf<DomainEvent>()

    @Version
    private var version: Long = 0L

    @Column(name = "is_deleted")
    protected var isDeleted: Boolean = false

    protected fun registerEvent(event: DomainEvent) {
        domainEvents.add(event)
    }

    open val events: List<DomainEvent>
        get() = domainEvents

    override fun equals(other: Any?): Boolean {
        other ?: return false

        if (this === other) return true

        if (javaClass != ProxyUtils.getUserClass(other)) return false

        other as AbstractEntity

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id)"
    }
}