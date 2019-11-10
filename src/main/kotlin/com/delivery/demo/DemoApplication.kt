package com.delivery.demo

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.demo.restaurant.model.Address
import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.schema.configuration.ObjectMapperConfigured
import springfox.documentation.service.ApiKey
import springfox.documentation.service.AuthorizationScope
import springfox.documentation.service.SecurityReference
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spi.service.contexts.SecurityContext
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


@SpringBootApplication
class DemoApplication(
    val restaurantRepository: RestaurantRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val bg = Restaurant(
            name = "Burger King",
            address = Address("address", "city", "country")
        )
        bg.dishes.add(Dish(name = "Whopper", restaurant = bg, price = Money.of(CurrencyUnit.EUR, 5.50)))
        bg.dishes.add(Dish(name = "Fries", restaurant = bg, price = Money.of(CurrencyUnit.EUR, 1.50)))

        restaurantRepository.save(bg)

        val donerPlace = Restaurant(
            name = "Döner place",
            address = Address("Street 2", "City", "Country")
        )
        donerPlace.dishes.add(Dish(name = "Döner", restaurant = donerPlace, price = Money.of(CurrencyUnit.EUR, 4.50)))
        donerPlace.dishes.add(Dish(name = "Kebap", restaurant = donerPlace, price = Money.of(CurrencyUnit.EUR, 3.50)))
        restaurantRepository.save(donerPlace)
    }

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration
@EnableSwagger2
class SwaggerConfig : ApplicationListener<ObjectMapperConfigured> {

    override fun onApplicationEvent(event: ObjectMapperConfigured) {
        val moneyModule = SimpleModule()
        moneyModule.addSerializer(Money::class.java, JacksonConfiguration.MoneySerializer())
        moneyModule.addDeserializer(Money::class.java, JacksonConfiguration.MoneyDeserializer())
        event.objectMapper.registerModule(moneyModule)
        event.objectMapper.registerModule(KotlinModule())
    }

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.delivery.demo"))
            .paths(PathSelectors.any())
            .build()
            .directModelSubstitute(Money::class.java, JacksonConfiguration.MoneyView::class.java)
            .produces(setOf("application/json"))
            .consumes(setOf("application/json"))
            .securityContexts(listOf(securityContext()))
            .securitySchemes(listOf(apiKey()))
    }

    private fun apiKey(): ApiKey {
        return ApiKey("JWT", "Authorization", "header")
    }

    private fun securityContext(): SecurityContext {
        return SecurityContext.builder()
            .securityReferences(defaultAuth())
            .forPaths(PathSelectors.regex("/api/.*"))
            .build()
    }

    fun defaultAuth(): List<SecurityReference> {
        val authorizationScope = AuthorizationScope("global", "accessEverything")
        val authorizationScopes = arrayOfNulls<AuthorizationScope>(1)
        authorizationScopes[0] = authorizationScope
        return listOf(
            SecurityReference("JWT", authorizationScopes)
        )
    }
}

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