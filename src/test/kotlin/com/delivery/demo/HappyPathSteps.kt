package com.delivery.demo

import com.delivery.demo.basket.AddItemToBasketInput
import com.delivery.demo.basket.BasketDTO
import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.RestaurantDTO
import com.delivery.demo.restaurant.RestaurantRepository
import com.delivery.restaurant.Address
import com.delivery.restaurant.model.Restaurant
import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.cucumber.spring.CucumberTestContext
import org.joda.money.CurrencyUnit
import org.joda.money.Money
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Scope
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [DemoApplication::class], loader = SpringBootContextLoader::class)
class HappyPathSteps : En {

    private val restTemplate = RestTemplate()

    val TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlJVUkNNa0ZHTWpsR1FUZ3lPVU5CUkVFMU9EaEJNRGt3UlRaRVEwRkZPVU0xUkRVelF6aERRZyJ9.eyJpc3MiOiJodHRwczovL2Rldi1kZWxpdmVyeS5hdXRoMC5jb20vIiwic3ViIjoiR1l1OHFydUpoTnpMTTFKZWlQaWNVWFpmSXljNjNlUXZAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vZGVsaXZlcnkvYXBpIiwiaWF0IjoxNTc3MDM4MjAyLCJleHAiOjE1NzcxMjQ2MDIsImF6cCI6IkdZdThxcnVKaE56TE0xSmVpUGljVVhaZkl5YzYzZVF2IiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.S96iI5AlwIlP3I4a9Hx0nz7x4TnzdRKW7uUtuIkJU7fWps6qkG7MA5ZOfWW1V6ZpH5IX-dHolSs59iBTD1nqm7dzIGkTnubx59hW03OMF-fy9QOu9fB2SiBK_fKcIR3ZOGiPRWEfJDafQek3MxSosxoGOAGDzjEwlPNg3CpYzIG-b1qX2v5JU-CeUUqY_drO_RRu5DS24DejVrYxWeCPI1u0CD5khTbjQc0kDph_o0IIIIzM5-LykzIe44IEBJtwElMxKSDDmuvCN1xdXwOyjshJ0ysc6qHN9rpDlwtSg1vWSyZg8mShVgbxYvjNci8vivM8vvthCzAHhdhBEVldpg"

    @Autowired
    lateinit var world: HappyPathWorld

    @Autowired
    lateinit var restaurantRepository: RestaurantRepository

    @LocalServerPort
    var serverPort = 0

    init {
        Given("^restaurant \"(.+)\" with following dishes$") { restaurantName: String, dataTable: DataTable ->
            val restaurant = Restaurant(
                id = UUID.randomUUID(),
                name = restaurantName,
                address = Address("Fake", "Fake", "Fake"),
                currency = CurrencyUnit.USD
            )
            dataTable.asMaps()
                .forEach {
                    val dishName = it.getValue("dish")
                    val price = it.getValue("price").toDouble()
                    restaurant.addDish(dishName, Money.of(restaurant.currency, price))
                }
            restaurantRepository.save(restaurant)
        }

        Given("A signed-in user") {
            // TODO: fetch new token
            restTemplate.interceptors = listOf(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.add("Authorization", "Bearer $TOKEN")
                    return@ClientHttpRequestInterceptor execution.execute(request, body)
                }
            )
        }
        Given("user's basket is empty") {
            val response = restTemplate.getForObject<BasketDTO?>("http://localhost:$serverPort/api/basket")
            assert(response == null)
        }
        When("user browses list of restaurants") {
            world.restaurants =
                restTemplate.getForObject<Array<RestaurantDTO>>("http://localhost:$serverPort/api/restaurants")
        }
        When("^user browses dishes of \"(.*)\" restaurant$") { restaurantName: String ->
            world.selectedRestaurant = world.restaurants.first { it.name == restaurantName }
            world.dishes =
                restTemplate.getForObject<Array<DishDTO>>("http://localhost:$serverPort/api/restaurants/${world.selectedRestaurant.id}/dishes")
        }
        When("^user adds (.*) \"(.*)\" to basket$") { quantity: Int, dishName: String ->
            val dish = world.dishes.first { it.name == dishName }
            val input = AddItemToBasketInput(
                dishId = UUID.fromString(dish.id),
                restaurantId = UUID.fromString(world.selectedRestaurant.id),
                quantity = quantity
            )
            world.basket =
                restTemplate.postForObject<BasketDTO>("http://localhost:$serverPort/api/basket/addItem", input)
        }
        Then("user's basket should not be empty") {
            val response = restTemplate.getForObject<BasketDTO>("http://localhost:$serverPort/api/basket")
            assert(response.items.isNotEmpty())
        }
        Then("user's basket total amount should be {double}") { totalAmount: Double ->
            // Double comparison, whatever ¯\_(ツ)_/¯
            assert(world.basket.totalAmount.amount == totalAmount)
        }
        When("user performs checkout") {
            val response = restTemplate.postForObject<String>("http://localhost:$serverPort/api/basket/checkout")
            println(response)
        }

    }
}

@Component
@Scope(CucumberTestContext.SCOPE_CUCUMBER_GLUE)
class HappyPathWorld {
    lateinit var restaurants: Array<RestaurantDTO>
    lateinit var selectedRestaurant: RestaurantDTO
    lateinit var dishes: Array<DishDTO>
    lateinit var basket: BasketDTO
}