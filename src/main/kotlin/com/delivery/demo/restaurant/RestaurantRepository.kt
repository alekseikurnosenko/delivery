package com.delivery.demo.restaurant

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface RestaurantRepository: JpaRepository<Restaurant, UUID>