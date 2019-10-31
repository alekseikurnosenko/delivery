package com.delivery.demo.restaurant.model

import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurants")
class Restaurant(
        @Id @GeneratedValue(strategy = GenerationType.AUTO, generator = "pg-uuid") var id: UUID? = null,
        var name: String,
        @Embedded var address: Address
)

@Embeddable
class Address(
        var address: String,
        var city: String,
        var country: String
)