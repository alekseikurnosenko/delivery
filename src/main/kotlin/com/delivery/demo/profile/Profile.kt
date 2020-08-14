package com.delivery.demo.profile

import com.delivery.demo.Address
import org.hibernate.annotations.Where

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "profiles")
@Where(clause = "is_deleted = false")
data class Profile(
    @Id val id: UUID = UUID.randomUUID()
) {
    var userId: String? = null
    var deliveryAddress: Address? = null
    var paymentMethodId: String? = null
}