package com.delivery.demo.notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.*
import org.jboss.logging.Logger
import java.util.*


interface FirebaseService {
    data class PushNotification(
            val token: String,
            val title: String,
            val body: String,
            val topic: String? = null,
            val data: Map<String, String>? = null
    )

    fun sendPushNotification(notification: FirebaseService.PushNotification)
}

class NoopFirebaseService : FirebaseService {
    override fun sendPushNotification(notification: FirebaseService.PushNotification) {

    }
}

class FirebaseServiceImpl(
        encodedServiceAccount: String
) : FirebaseService {

    val logger: Logger = Logger.getLogger(FirebaseService::class.java)

    init {
        val content = String(Base64.getUrlDecoder().decode(encodedServiceAccount));
        val credentials = GoogleCredentials.fromStream(content.byteInputStream())
        val options = FirebaseOptions.Builder()
                .setCredentials(credentials)
                .setDatabaseUrl("https://testab-d80a2.firebaseio.com")
                .build()

        FirebaseApp.initializeApp(options)
    }

    override fun sendPushNotification(notification: FirebaseService.PushNotification) {
        val builder = Message.builder()
                .setNotification(Notification.builder().setTitle(notification.title).setBody(notification.body).build())
                .setToken(notification.token)
                .setAndroidConfig(getAndroidConfig(notification.topic))
                .setApnsConfig(getApnsConfig(notification.topic))

        if (notification.data != null) {
            builder.putAllData(notification.data.toMutableMap())
        }

        try {
            FirebaseMessaging.getInstance().send(builder.build())
        } catch (e: Exception) {
            logger.error("Failed to send push notification", e)
        }
    }

    private fun getAndroidConfig(topic: String?): AndroidConfig {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setCollapseKey(topic)
                .build()
    }

    private fun getApnsConfig(topic: String?): ApnsConfig {
        return ApnsConfig.builder()
                .setAps(Aps.builder().setThreadId(topic).build()).build()
    }
}
