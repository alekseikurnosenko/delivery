package com.delivery.demo.profile

import com.delivery.demo.AbstractEntity
import com.delivery.demo.Address
import org.hibernate.annotations.Where
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "profiles")
@Where(clause = "is_deleted = false")
class Profile : AbstractEntity() {
    var userId: String? = null
    var deliveryAddress: Address? = null
    var paymentMethodId: String? = null
}