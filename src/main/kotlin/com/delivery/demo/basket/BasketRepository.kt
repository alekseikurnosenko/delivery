package com.delivery.demo.basket

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface BasketRepository : JpaRepository<Basket, UUID>