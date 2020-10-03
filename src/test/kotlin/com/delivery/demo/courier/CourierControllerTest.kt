package com.delivery.demo.courier

import com.delivery.demo.EventPublisher
import com.delivery.demo.courier
import com.delivery.demo.delivery.DeliveryRequestAccepted
import com.delivery.demo.order
import com.delivery.demo.order.OrderRepository
import com.nhaarman.mockitokotlin2.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import java.util.*

@ExtendWith(MockitoExtension::class)
internal class CourierControllerTest {

    @Mock
    lateinit var eventPublisher: EventPublisher

    @Mock
    lateinit var courierRepository: CourierRepository

    @Mock
    lateinit var orderRepository: OrderRepository

    @Mock
    lateinit var courierLocationRepository: CourierLocationRepository

    @InjectMocks
    lateinit var courierController: CourierController

    @Nested
    @DisplayName("Accepting delivery request")
    inner class AcceptDeliveryRequest {

        val order = order()
        val courier = courier()

        @BeforeEach
        fun setUp() {
            whenever(orderRepository.findById(order.id)).thenReturn(Optional.of(order))
            whenever(courierRepository.findById(any())).then {
                when (it.getArgument<UUID>(0)) {
                    courier.id -> Optional.of(courier)
                    else -> Optional.empty()
                }
            }
        }

        @Test
        @MockitoSettings(strictness = Strictness.LENIENT)
        fun `fails for unknown courier`() {
            assertThrows(CourierNotFoundException::class.java) {
                // Complains: "stubbed method is intentionally invoked with different arguments by code under test"
                courierController.acceptDeliveryRequest(UUID.randomUUID(), order.id)
            }
        }

        @Test
        fun `fails if courier wasn't requested`() {
            // Part of domain test?
            // TODO: domain error
            assertThrows(Exception::class.java) {
                courierController.acceptDeliveryRequest(courier.id, order.id)
            }
        }

        @Nested
        @DisplayName("When a courier was requested")
        inner class WithOrderAndCourier {

            @BeforeEach
            fun setUp() {
                order.delivery.requestCourier(courier)
            }

            @Test
            // Why?
            fun `assigns order to this courier`() {
                courierController.acceptDeliveryRequest(courier.id, order.id)

                assertThat(order.delivery.assignedCourier).isEqualTo(courier)
            }

            @Test
            @Disabled("Not true! Would be added after some time only!")
            fun `adds this order to couriers active orders`() {
                courierController.acceptDeliveryRequest(courier.id, order.id)

                assertThat(courier.activeOrders).contains(order)
            }

            @Test
            fun `emits an event that request was accepted`() {
                courierController.acceptDeliveryRequest(courier.id, order.id)

                verify(eventPublisher).publish(argThat { any { it is DeliveryRequestAccepted } }, eq("default"))
            }
        }
    }
}
