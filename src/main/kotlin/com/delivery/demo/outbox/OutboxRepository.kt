package com.delivery.demo.outbox

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OutboxRepository : CrudRepository<OutboxMessage, UUID>