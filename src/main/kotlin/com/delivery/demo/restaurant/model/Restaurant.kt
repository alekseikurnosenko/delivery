package com.delivery.demo.restaurant.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.money.Money
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "pg-uuid")
    var id: UUID? = null,
    var name: String,
    @OneToMany(mappedBy = "restaurant", cascade = [CascadeType.ALL])
    var dishes: MutableList<Dish> = mutableListOf(),
    @Embedded var
    address: Address
)

@Entity
class Dish(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "pg-uuid")
    var id: UUID? = null,
    var name: String,
    var price: Money,
    @OneToOne
    @JoinColumn(name = "restaurant_id")
    @JsonIgnore
    var restaurant: Restaurant
)

@Embeddable
class Address(
    var address: String,
    var city: String,
    var country: String
)