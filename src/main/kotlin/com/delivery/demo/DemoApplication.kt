package com.delivery.demo

import com.delivery.demo.courier.CourierAdded
import com.delivery.demo.courier.CourierLocationUpdated
import com.delivery.demo.courier.CourierShiftStarted
import com.delivery.demo.courier.CourierShiftStopped
import com.delivery.demo.delivery.DeliveryRequested
import com.delivery.demo.notification.FirebaseTokenInformation
import com.delivery.demo.notification.FirebaseTokenRepository
import com.delivery.demo.order.*
import com.delivery.demo.restaurant.RestaurantAdded
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverterContext
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.joda.money.format.MoneyFormatterBuilder
import org.springdoc.core.customizers.OpenApiCustomiser
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.MediaType
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@SpringBootApplication(scanBasePackages = ["com.delivery"])
@EnableJpaRepositories(basePackages = ["com.delivery"])
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EntityScan(basePackages = ["com.delivery"])
@EnableScheduling
@EnableRetry
class DemoApplication {

    @Bean("auditorAware")
    fun auditorAware(): AuditorAware<Unit> = AuditorAware {
        Optional.empty<Unit>()
    }

    @Bean
    fun taskScheduler(): TaskScheduler {
        return ConcurrentTaskScheduler()
    }

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
                .components(
                        Components().addSecuritySchemes(
                                "JWT",
                                SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(SecurityRequirement().addList("JWT"))
    }

    // We can try to use reflection and check for all classes implementing an interface instead
    @Bean
    @Qualifier("publishableEvents")
    fun publishableEvents(): List<Class<out DomainEvent>> = listOf(
            RestaurantAdded::class.java,
            OrderPreparationStarted::class.java,
            OrderPreparationFinished::class.java,
            OrderPlaced::class.java,
            OrderAssigned::class.java,
            OrderPickedUp::class.java,
            OrderDelivered::class.java,
            OrderCanceled::class.java,
            CourierLocationUpdated::class.java,
            CourierShiftStarted::class.java,
            CourierShiftStopped::class.java,
            CourierAdded::class.java,
            DeliveryRequested::class.java
    )

    @Bean
    fun customerGlobalHeaderOpenApiCustomiser(@Qualifier("publishableEvents") events: List<Class<out DomainEvent>>): OpenApiCustomiser =
            OpenApiCustomiser {
                // Enrich generated API definition with event schemas

                for (event in events) {
                    val schemas = ModelConverters.getInstance().read(event)
                    it.components.schemas.putAll(schemas)
                }
            }
//
//    @Bean
//    fun requestDumperFilter(): FilterRegistrationBean<RequestDumperFilter> {
//        val registration = FilterRegistrationBean<RequestDumperFilter>()
//        val requestDumperFilter = RequestDumperFilter()
//        registration.filter = requestDumperFilter
//        registration.addUrlPatterns("/*")
//        return registration
//    }
}

interface DomainEvent {
    val routingKey: String
        get() = "event"
}

interface EventPublisher {
    fun publish(events: List<DomainEvent>, topic: String = "default")
}

interface EventSubscriber {
    fun <T : DomainEvent> subscribe(eventType: Class<T>, topic: String = "default", handler: (T) -> Unit): Subscription
    fun <T : DomainEvent> subscribeAll(
            eventTypes: List<Class<out T>>,
            topic: String = "default",
            handler: (T) -> Unit
    ): Subscription
}

interface Subscription {
    fun unsubscribe()
}

@Configuration
internal class WebMvcConfiguration : WebMvcConfigurer {
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }
}

@Configuration
class JacksonConfiguration {

    @Bean
    fun objectMapper(): ObjectMapper {
        val moneyModule = SimpleModule()
        moneyModule.addSerializer(Money::class.java, MoneySerializer())
        moneyModule.addDeserializer(Money::class.java, MoneyDeserializer())

        moneyModule.addSerializer(CurrencyUnit::class.java, object : JsonSerializer<CurrencyUnit>() {
            override fun serialize(value: CurrencyUnit, gen: JsonGenerator, serializers: SerializerProvider) {
                gen.writeString(value.code)
            }
        })
        moneyModule.addDeserializer(CurrencyUnit::class.java, object : JsonDeserializer<CurrencyUnit>() {
            override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CurrencyUnit {
                val code = p.readValueAs(String::class.java)
                return CurrencyUnit.of(code)
            }

        })

        return Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(moneyModule)
                .build()
    }

    data class MoneyView(
            val amount: Double,
            val currencyCode: String,
            val formatted: String
    )


    class MoneySerializer : JsonSerializer<Money>() {

        override fun serialize(value: Money, gen: JsonGenerator, serializers: SerializerProvider) {
            // Need to get user local here?
            val formatter =
                    MoneyFormatterBuilder().appendCurrencySymbolLocalized().appendAmountLocalized().toFormatter()
            val view = MoneyView(
                    amount = value.amount.toDouble(),
                    currencyCode = value.currencyUnit.code,
                    formatted = formatter.print(value)
            )
            gen.writeObject(view)
        }
    }

    class MoneyDeserializer : JsonDeserializer<Money>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Money {
            val view = p.readValueAs(MoneyView::class.java)
            val currencyUnit = CurrencyUnit.of(view.currencyCode)
            return Money.of(currencyUnit, view.amount)
        }
    }

}

@Component
class MoneyConverter : ModelConverter {

    override fun resolve(
            type: AnnotatedType,
            context: ModelConverterContext,
            chain: MutableIterator<ModelConverter>
    ): Schema<*>? {
        val typeName = type.type.typeName

        var newType = type
        if (type.isSchemaProperty) {
            Json.mapper().constructType(type.type)?.let {
                val cls = it.rawClass
                if (Money::class.java.isAssignableFrom(cls)) {
                    newType = AnnotatedType(JacksonConfiguration.MoneyView::class.java)
                }
            }
        }

        return if (chain.hasNext()) {
            chain.next().resolve(newType, context, chain)
        } else {
            null
        }
    }

    companion object {
        init {
            ModelConverters.getInstance().addConverter(MoneyConverter())
        }
    }
}

@Component
class FirebaseTokenInterceptor(
        val firebaseTokenRepository: FirebaseTokenRepository
) : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

        val userId = SecurityContextHolder.getContext().authentication?.principal as? String
        val firebaseToken = request.getHeader("X-FirebaseToken")

        if (userId != null && firebaseToken != null) {
            firebaseTokenRepository.findById(userId)
                    .ifPresentOrElse({
                        if (it.firebaseToken != firebaseToken) {
                            it.firebaseToken = firebaseToken
                            firebaseTokenRepository.save(it)
                        }
                    }, {
                        firebaseTokenRepository.save(FirebaseTokenInformation(userId, firebaseToken))
                    })
        }

        return super.preHandle(request, response, handler)
    }
}

@Configuration
class WebMvcConfig(
        val firebaseTokenInterceptor: FirebaseTokenInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(firebaseTokenInterceptor);
    }
}