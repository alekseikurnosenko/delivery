package com.delivery.demo.order

import com.delivery.demo.EventPublisher
import com.delivery.demo.payment.ChargeResult
import com.delivery.demo.payment.PaymentService
import com.delivery.demo.profile.ProfileRepostiory
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProcessPlacedOrderUseCase(
    private val orderRepository: OrderRepository,
    private val paymentService: PaymentService,
    private val eventPublisher: EventPublisher,
    private val profileRepository: ProfileRepostiory
) {

    @Transactional
    @RabbitListener(queues = [OrderPlaced.queue])
    fun onOrderPlaced(orderPlaced: OrderPlaced) {
        val order = orderRepository.findById(orderPlaced.orderId)
            .orElseThrow {
                AmqpRejectAndDontRequeueException("Order(id=${orderPlaced.orderId}) not found")
            }

        val profile = profileRepository.findByUserId(order.userId)
            .orElseThrow {
                AmqpRejectAndDontRequeueException("Profile(userId=${order.userId}) not found")
            }

        val paymentMethodId = profile.paymentMethodId
        if (paymentMethodId == null) {
            order.cancel("No payment method set")
            eventPublisher.publish(order.events)
            return
        }

        // Charge user
        // Assume that it's idempotent
        when (paymentService.charge(orderPlaced.userId, paymentMethodId, orderPlaced.totalAmount)) {
            is ChargeResult.Success -> order.confirmPaid()
            is ChargeResult.NotEnoughFunds -> order.cancel("Not enough funds")
            is ChargeResult.UnknownPaymentMethod -> order.cancel("Unknown payment method: $paymentMethodId")
        }

        // Would publish orderPaid/orderCanceled event
        eventPublisher.publish(order.events)
    }
}