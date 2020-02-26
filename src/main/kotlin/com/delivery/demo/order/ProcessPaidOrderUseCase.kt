package com.delivery.demo.order

import com.delivery.demo.EventPublisher
import com.delivery.demo.delivery.DeliveryService
import com.delivery.demo.payment.PaymentService
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ProcessPaidOrderUseCase(
    val orderRepository: OrderRepository,
    val deliveryService: DeliveryService,
    val paymentService: PaymentService,
    val eventPublisher: EventPublisher
) {

    @Transactional
    @RabbitListener(queues = ["order.paid"])
    fun onOrderPaid(orderPaid: OrderPaid) {
        val order = orderRepository.findById(orderPaid.orderId)
            .orElseThrow {
                // Not supposed to happen
                AmqpRejectAndDontRequeueException("Order(id=${orderPaid.orderId} not found")
            }

        // Try search for couriers?
        deliveryService.findCourierForOrder(order)

        // If we cannot to do it at all - refund and cancel event
//        if (courier == null) {
//            // Assume that it's idempotent
//            paymentService.refund(order.userId, "TODO", order.totalAmount)
//
//            // order.cancel?
//            // Publish event?
//            return
//        }

        // Somehow notify the restaurant?
        // What would be the action though?
        // Or just update the order state?
        // So, as the restaurant I don't care about unpaid-orders
        // Wait, what about cash payments 🤔
    }
}