package com.delivery.demo

import com.delivery.demo.basket.Basket
import com.delivery.demo.basket.BasketItem
import com.delivery.demo.courier.Courier
import com.delivery.demo.courier.LatLng
import com.delivery.demo.order.Order
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import org.joda.money.CurrencyUnit
import org.joda.money.Money

fun address() = Address(LatLng(0f, 0f), "address", "city", "country")
fun restaurant() = Restaurant.new("restaurantAccountId", "name", address(), CurrencyUnit.CAD, null)
fun dish() = Dish("dishName", Money.of(CurrencyUnit.CAD, 3.5), null, restaurant())
fun basket() = Basket("userId", address(), restaurant())
fun order() = Order.place("userId", restaurant(), address(), listOf(BasketItem(dish(), basket(), 1)))
fun courier() = Courier.new("userId", "courierName")