package com.delivery.demo.order

import org.springframework.data.repository.CrudRepository
import java.util.*

interface OrderRepository : CrudRepository<Order, UUID> {
    fun findAllByOrderByCreatedDateDesc(): List<Order>
}