package com.delivery.demo.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RestaurantRepository : JpaRepository<Restaurant, UUID> {
    fun findByAccountId(userId: String): Optional<Restaurant>
}