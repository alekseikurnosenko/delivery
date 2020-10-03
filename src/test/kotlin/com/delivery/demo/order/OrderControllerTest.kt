package com.delivery.demo.order

import com.delivery.demo.RestResponsePage
import com.delivery.demo.order
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class OrderControllerTest {

    @Mock
    lateinit var orderRepository: OrderRepository
    @InjectMocks
    lateinit var controller: OrderController

    @Test
    fun getOrders() {
        val order = order()

        whenever(orderRepository.findAll(anyOrNull())).thenReturn(RestResponsePage(listOf(order)))

        val orders = controller.orders(PageRequest.of(1, 10))

        assertThat(orders.content.map { it.id }).contains(order.id.toString())
    }

}