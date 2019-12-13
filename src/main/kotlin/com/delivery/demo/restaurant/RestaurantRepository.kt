package com.delivery.demo.restaurant

import com.delivery.restaurant.model.Restaurant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RestaurantRepository: JpaRepository<Restaurant, UUID>