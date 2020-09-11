package com.delivery.demo.order

import com.delivery.demo.Address
import com.delivery.demo.RestResponsePage
import com.delivery.demo.basket.Basket
import com.delivery.demo.basket.BasketItem
import com.delivery.demo.courier.LatLng
import com.delivery.demo.restaurant.Dish
import com.delivery.demo.restaurant.Restaurant
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.data.domain.PageRequest

@RunWith(MockitoJUnitRunner::class)
class OrderControllerTest {

    @Mock
    lateinit var orderRepository: OrderRepository

    @InjectMocks
    lateinit var controller: OrderController

    fun address() = Address(LatLng(0f, 0f), "address", "city", "country")
    fun restaurant() = Restaurant.new("restaurantAccountId", "name", address(), CurrencyUnit.CAD, null)
    fun dish() = Dish("dishName", Money.of(CurrencyUnit.CAD, 3.5), null, restaurant())
    fun basket() = Basket("userId", address(), restaurant())
    fun order() = Order.place("userId", restaurant(), address(), listOf(BasketItem(dish(), basket(), 1)))

    @Test
    fun getOrders() {
        val order = order()

        whenever(orderRepository.findAll(anyOrNull())).thenReturn(RestResponsePage(listOf(order)))

        val orders = controller.orders(PageRequest.of(1, 10))

        assertThat(orders.content.map { it.id }).contains(order.id.toString())
    }

}