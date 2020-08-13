package com.delivery.demo.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import java.util.*

interface OrderRepository : CrudRepository<Order, UUID> {
    fun findAll(pageable: Pageable): Page<Order>
    fun findAllByOrderByCreatedDateDesc(): List<Order>
    fun findByRestaurantId(restaurantId: UUID): List<Order>
    fun findByStatusAndRestaurantId(status: OrderStatus, restaurantId: UUID): List<Order>
}