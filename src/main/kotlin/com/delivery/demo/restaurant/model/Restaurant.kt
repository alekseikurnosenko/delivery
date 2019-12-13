package com.delivery.restaurant.model

import com.delivery.restaurant.Address
import com.fasterxml.jackson.annotation.JsonIgnore
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id
    val id: UUID,
    @field:NotNull
    val name: String,
    @OneToMany(mappedBy = "", cascade = [CascadeType.ALL])
    private val _dishes: MutableList<Dish> = mutableListOf(),
    val minimumOrderAmount: Money?,
    @Embedded
    val address: Address,
    val currency: CurrencyUnit
) {

    val dishes: List<Dish>
        @NotNull
        get() = _dishes

    fun addDish(name: String, price: Money): Dish {
        val dish = Dish(
            id = UUID.randomUUID(),
            name = name,
            price = price,
            restaurant = this
        )
        _dishes.add(dish)
        return dish
    }
}

@Entity
class Dish(
    @Id
    val id: UUID,
    val name: String,
    val price: Money,
    @OneToOne
    @JoinColumn(name = "restaurant_id")
    @JsonIgnore
    val restaurant: Restaurant
)