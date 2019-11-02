package com.delivery.demo

import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.demo.restaurant.model.Address
import com.delivery.demo.restaurant.model.Dish
import com.delivery.demo.restaurant.model.Restaurant
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2
import java.util.*


@SpringBootApplication
class DemoApplication(
    val restaurantRepository: RestaurantRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val restaurant = Restaurant(
            id = UUID.fromString("00cf5f40-1c6c-4eab-8efc-41fb8a337a22"),
            name = "Test restaurant",
            address = Address("address", "city", "country")
        )
        restaurant.dishes.add(Dish(name = "Dish 1", restaurant = restaurant))

        restaurantRepository.save(restaurant)
    }

}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

@Configuration
@EnableSwagger2
class SwaggerConfig {
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("com.delivery.demo"))
            .paths(PathSelectors.any())
            .build()
            .produces(setOf("application/json"))
            .consumes(setOf("application/json"))
    }
}

@Configuration
internal class WebMvcConfiguration : WebMvcConfigurer {
    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON)
    }
}