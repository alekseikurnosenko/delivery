package com.delivery.demo.delivery

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface DeliveryRepository : JpaRepository<Delivery, UUID>