package com.delivery.demo.profile

import com.delivery.demo.Address
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "profiles")
data class Profile(
    @Id val id: UUID = UUID.randomUUID()
) {
    var name: String? = null
    var deliveryAddress: Address? = null
}