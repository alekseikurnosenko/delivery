package com.delivery.demo.notification

import com.delivery.demo.DomainEvent
import com.delivery.demo.courier.CourierRepository
import com.delivery.demo.delivery.DeliveryRequested
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class PushNotificationSender(
        val firebaseTokenRepository: FirebaseTokenRepository,
        val courierRepository: CourierRepository,
        val firebaseService: FirebaseService
) {

    @RabbitListener(queues = ["pushNotification"])
    fun onEvent(event: DomainEvent) {
        val notification = when (event) {
            is DeliveryRequested -> {
                val courier = courierRepository.findById(event.courierId).orElse(null) ?: return
                val token = firebaseTokenRepository.findById(courier.accountId).orElse(null) ?: return

                FirebaseService.PushNotification(
                        token.firebaseToken,
                        title = "New delivery request",
                        body = "Check the app for details"
                )
            }
            else -> return
        }

        firebaseService.sendPushNotification(notification)
    }
}