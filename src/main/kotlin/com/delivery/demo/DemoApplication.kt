package com.delivery.demo

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.Address
import com.delivery.restaurant.model.Restaurant
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*


@Component
class RestaurantsApplication(
    val restaurantRepository: RestaurantRepository
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        val bg = Restaurant(
            id = UUID.randomUUID(),
            name = "Burger King",
            address = Address("address", "city", "country")
        )
        bg.addDish(name = "Whopper", price = Money.of(CurrencyUnit.EUR, 5.50))
        bg.addDish(name = "Fries", price = Money.of(CurrencyUnit.EUR, 1.50))

        restaurantRepository.save(bg)

        val donerPlace = Restaurant(
            id = UUID.randomUUID(),
            name = "Döner place",
            address = Address("Street 2", "City", "Country")
        )
        donerPlace.addDish(name = "Döner", price = Money.of(CurrencyUnit.EUR, 4.50))
        donerPlace.addDish(name = "Kebap", price = Money.of(CurrencyUnit.EUR, 3.50))
        restaurantRepository.save(donerPlace)
    }

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@SpringBootApplication(scanBasePackages = ["com.delivery"])
@EnableJpaRepositories(basePackages = ["com.delivery"])
@EntityScan(basePackages = ["com.delivery"])
class DemoApplication {

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
    }
}

//
//@Configuration
//@EnableSwagger2
//class SwaggerConfig : ApplicationListener<ObjectMapperConfigured> {
//
//    override fun onApplicationEvent(event: ObjectMapperConfigured) {
//        val moneyModule = SimpleModule()
//        moneyModule.addSerializer(Money::class.java, JacksonConfiguration.MoneySerializer())
//        moneyModule.addDeserializer(Money::class.java, JacksonConfiguration.MoneyDeserializer())
//        event.objectMapper.registerModule(moneyModule)
//        event.objectMapper.registerModule(KotlinModule())
//    }
//
//    @Bean
//    fun api(): Docket {
//        return Docket(DocumentationType.SWAGGER_2)
//            .select()
//            .apis(RequestHandlerSelectors.basePackage("com.delivery.demo"))
//            .paths(PathSelectors.any())
//            .build()
//            .directModelSubstitute(Money::class.java, JacksonConfiguration.MoneyView::class.java)
//            .produces(setOf("application/json"))
//            .consumes(setOf("application/json"))
//            .securityContexts(listOf(securityContext()))
//            .securitySchemes(listOf(apiKey()))
//    }
//
//    private fun apiKey(): ApiKey {
//        return ApiKey("JWT", "Authorization", "header")
//    }
//
//    private fun securityContext(): SecurityContext {
//        return SecurityContext.builder()
//            .securityReferences(defaultAuth())
//            .forPaths(PathSelectors.regex("/api/.*"))
//            .build()
//    }
//
//    fun defaultAuth(): List<SecurityReference> {
//        val authorizationScope = AuthorizationScope("global", "accessEverything")
//        val authorizationScopes = arrayOfNulls<AuthorizationScope>(1)
//        authorizationScopes[0] = authorizationScope
//        return listOf(
//            SecurityReference("JWT", authorizationScopes)
//        )
//    }
//}

@Configuration
internal class WebMvcConfiguration : WebMvcConfigurer {
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }
}

@Configuration
internal class JacksonConfiguration {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val objectMapper = ObjectMapper()
        val moneyModule = SimpleModule()
        moneyModule.addSerializer(Money::class.java, MoneySerializer())
        moneyModule.addDeserializer(Money::class.java, MoneyDeserializer())
        objectMapper.registerModule(moneyModule)
        objectMapper.registerModule(KotlinModule())
        return objectMapper
    }

    data class MoneyView(
        val amount: Double,
        val currency: CurrencyUnit
    )

    class MoneySerializer : JsonSerializer<Money>() {
        override fun serialize(value: Money, gen: JsonGenerator, serializers: SerializerProvider) {
            gen.writeObject(MoneyView(value.amount.toDouble(), value.currencyUnit))
        }
    }

    class MoneyDeserializer : JsonDeserializer<Money>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Money {
            val view = p.readValueAs(MoneyView::class.java)
            return Money.of(view.currency, view.amount)
        }
    }

}