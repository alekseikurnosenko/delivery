package com.delivery.demo

import com.delivery.demo.basket.AddItemToBasketInput
import com.delivery.demo.basket.BasketDTO
import com.delivery.demo.courier.CourierDTO
import com.delivery.demo.courier.LatLng
import com.delivery.demo.courier.UpdateLocationInput
import com.delivery.demo.delivery.DeliveryRequested
import com.delivery.demo.notification.WebSocketMessage
import com.delivery.demo.order.*
import com.delivery.demo.profile.Profile
import com.delivery.demo.restaurant.CreateDishInput
import com.delivery.demo.restaurant.CreateRestaurantInput
import com.delivery.demo.restaurant.DishDTO
import com.delivery.demo.restaurant.RestaurantDTO
import com.fasterxml.jackson.databind.ObjectMapper
import io.cucumber.datatable.DataTable
import io.cucumber.java8.En
import io.cucumber.spring.CucumberTestContext
import org.assertj.core.api.Assertions.assertThat
import org.joda.money.CurrencyUnit
import org.junit.jupiter.api.fail
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
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@ContextConfiguration(classes = [DemoApplication::class, ApplicationConfig::class, TestConfiguration::class], loader = SpringBootContextLoader::class)
class HappyPathSteps : En {

    private val restTemplate = RestTemplate()

    @Autowired
    lateinit var auth0Client: Auth0Client

    @Autowired
    lateinit var world: HappyPathWorld

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var courierClientFactory: CourierClientFactory

    lateinit var token: String
    lateinit var session: WebSocketSession

    @LocalServerPort
    var serverPort = 80
    var endpoint = "localhost"
//    var endpoint = "enigmatic-garden-23553.herokuapp.com"

    private val locationMap = mapOf(
            "PointA" to LatLng(0.0f, 0.0f),
            "PointB" to LatLng(5.0f, 5.0f)
    )

    init {
        Before { _ ->
            //            val token = auth0Client.getToken()
            try {
                auth0Client.createUser("restaurant@delivery.com", "password")
            } catch (e: Throwable) {

            }
            val token = auth0Client.getUserToken("restaurant@delivery.com", "password")
            restTemplate.interceptors = listOf(
                    ClientHttpRequestInterceptor { request, body, execution ->
                        request.headers.add("Authorization", "Bearer $token")
                        return@ClientHttpRequestInterceptor execution.execute(request, body)
                    }
            )

            val client = StandardWebSocketClient()
            session = client.doHandshake(object : TextWebSocketHandler() {
                override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                    val socketMessage = objectMapper.readValue(message.payload, WebSocketMessage::class.java)
                    val event = objectMapper.readValue(socketMessage.payload, Class.forName(socketMessage.type))
                    world.events.add(event)
                }
            }, WebSocketHttpHeaders(), URI.create("ws://$endpoint:$serverPort/ws")).get()

            session.sendMessage(TextMessage("Hey"))
        }
        Given("^restaurant \"(.+)\" located near \"(.+)\" with following dishes$") { restaurantName: String, locationName: String, dataTable: DataTable ->
            val input = CreateRestaurantInput(
                    name = restaurantName,
                    address = Address(locationMap.getValue(locationName), "Fake", "Fake", "Fake"),
                    currency = CurrencyUnit.USD.code
            )
            val restaurant = restTemplate.postForObject<RestaurantDTO>(api("/restaurants"), input)
            dataTable.asMaps()
                    .map {
                        CreateDishInput(
                                name = it.getValue("dish"),
                                price = it.getValue("price").toDouble()
                        )
                    }
                    .forEach {
                        restTemplate.postForObject<DishDTO>(api("/restaurants/${restaurant.id}/dishes"), it)
                    }
        }
        Given("A signed-in user") {
            // Do we like need two restTemplates with different headers?
            // Aka, on service one, one normal one?
        }
        Given("user's basket is empty") {
            val response = restTemplate.getForObject<BasketDTO?>(api("/basket"))
            assert(response == null)
        }
        When("user browses list of restaurants") {
            world.restaurants =
                    restTemplate.getForObject<Array<RestaurantDTO>>(api("/restaurants"))
        }
        When("^user browses dishes of \"(.*)\" restaurant$") { restaurantName: String ->
            world.selectedRestaurant = world.restaurants.first { it.name == restaurantName }
            world.dishes =
                    restTemplate.getForObject<Array<DishDTO>>(api("/restaurants/${world.selectedRestaurant.id}/dishes"))
        }
        When("^user adds (.*) \"(.*)\" to basket$") { quantity: Int, dishName: String ->
            val dish = world.dishes.first { it.name == dishName }
            val input = AddItemToBasketInput(
                    dishId = UUID.fromString(dish.id),
                    restaurantId = UUID.fromString(world.selectedRestaurant.id),
                    quantity = quantity
            )
            world.basket =
                    restTemplate.postForObject<BasketDTO>(api("/basket/addItem"), input)
        }
        Then("user's basket should not be empty") {
            val response = restTemplate.getForObject<BasketDTO>(api("/basket"))
            assertThat(response.items).isNotEmpty
        }
        Then("^user's basket total amount should be (.+)") { totalAmount: Double ->
            // Double comparison, whatever ¯\_(ツ)_/¯
            assertThat(world.basket.totalAmount.amount).isEqualTo(totalAmount)
        }
        When("user performs checkout") {
            world.order = restTemplate.postForObject<OrderDTO>(api("/basket/checkout"))
        }
        Given("^a courier \"(.+)\"") { courierName: String ->
            world.couriers[courierName] = courierClientFactory.create(courierName, endpoint, serverPort)
        }
        Given("^\"(.+)\" is on shift") { courierName: String ->
            val courierId = world.couriers.getValue(courierName)
            restTemplate.postForObject<CourierDTO>(api("/couriers/$courierId/startShift"))
        }
        Given("^\"(.+)\" updated his location to be near \"(.+)\"") { courierName: String, locationName: String ->
            val courierId = world.couriers.getValue(courierName)
            val input = UpdateLocationInput(locationMap.getValue(locationName))
            restTemplate.postForObject<Void?>(api("/couriers/$courierId/location"), input)
        }
        Then("^\"(.+)\" receives a delivery request") { courierName: String ->
            val courier = world.couriers.getValue(courierName)
            retry {
                val deliveryRequested = courier.events.filterIsInstance(DeliveryRequested::class.java).first()
                assertThat(deliveryRequested.courierId.toString()).isEqualTo(courier.id)
            }
        }
        Then("^\"(.+)\" is assigned to deliver this order") { courierName: String ->
            retry {
                val courierId = world.couriers.getValue(courierName)
                val orders = restTemplate.getForObject<Array<OrderDTO>>(api("/couriers/$courierId/orders"))
                assertThat(orders.map { it.id }).contains(world.order.id)
            }
        }
        Then("^\"(.+)\" receives this order") { restaurantName: String ->
            val restaurantId = world.restaurants.first { it.name == restaurantName }.id
            val orders = restTemplate.getForObject<Array<OrderDTO>>(api("/restaurants/$restaurantId/orders"))
            assertThat(orders.map { it.id }).contains(world.order.id)
        }
        When("^\"(.+)\" starts to prepare this order") { restaurantName: String ->
            val restaurantId = world.restaurants.first { it.name == restaurantName }.id
            val orderId = world.order.id
            restTemplate.postForObject<OrderDTO>(api("/restaurants/$restaurantId/orders/$orderId/startPreparing"))
        }
        When("^\"(.+)\" finishes preparing this order") { restaurantName: String ->
            val restaurantId = world.restaurants.first { it.name == restaurantName }.id
            val orderId = world.order.id
            restTemplate.postForObject<OrderDTO>(api("/restaurants/$restaurantId/orders/$orderId/finishPreparing"))
        }
        Then("^\"(.+)\" is notified that order preparation started") { courierName: String ->
            retry {
                val events = world.events.filterIsInstance<OrderPreparationStarted>().first()
                assertThat(events.orderId.toString()).isEqualTo(world.order.id)
            }
        }
        Then("^\"(.+)\" is notified that order preparation finished") { courierName: String ->
            retry {
                val events = world.events.filterIsInstance<OrderPreparationFinished>().first()
                assertThat(events.orderId.toString()).isEqualTo(world.order.id)
            }
        }
        When("^\"(.+)\" confirm order pickup") { courierName: String ->
            val courierId = world.couriers.getValue(courierName)
            val orderId = world.order.id
            restTemplate.postForObject<OrderDTO>(api("/couriers/$courierId/orders/$orderId/confirmPickup"))
        }
        When("^\"(.+)\" confirm order dropoff") { courierName: String ->
            val courierId = world.couriers.getValue(courierName)
            val orderId = world.order.id
            restTemplate.postForObject<OrderDTO>(api("/couriers/$courierId/orders/$orderId/confirmDropoff"))
        }
        Then("^user can see their order as \"(.+)\"") { status: String ->
            val orderId = world.order.id
            val order = restTemplate.getForObject<OrderDTO>(api("/orders/$orderId"))
            val desiredState = when (status) {
                "placed" -> OrderStatus.Placed
                "being prepared" -> OrderStatus.Preparing
                "being picked up" -> OrderStatus.AwaitingPickup
                "in delivery" -> OrderStatus.InDelivery
                "delivered" -> OrderStatus.Delivered
                else -> fail("Unknown order status")
            }
            assertThat(order.status).isEqualTo(desiredState)
        }
        When("^user sets their address to be near \"(.+)\"") { locationName: String ->
            val address = Address(locationMap.getValue(locationName), "Fake", "Fake", "Fake")
            restTemplate.postForObject<Profile>(api("/profile/address"), address)

        }
        When("user sets valid payment method") {
            val input = mapOf("paymentMethodId" to "PAYMENT_METHOD_SUCCESS")
            restTemplate.postForObject(api("/profile/payment_method"), input)
        }
        When("^\"(.+)\" accepts this delivery request") { courierName: String ->
            world.couriers.getValue(courierName).acceptCurrentDeliveryRequest()
        }
    }

    fun api(path: String) = "http://$endpoint:$serverPort$path"
}

fun retry(handler: () -> Unit) {
    val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
    var throwable: Throwable? = null
    while (System.nanoTime() < end) {
        try {
            handler()
            return
        } catch (e: Throwable) {
            throwable = e
        }
    }
    throw throwable ?: Exception("Timed out")
}

@Component
@Scope(CucumberTestContext.SCOPE_CUCUMBER_GLUE)
class HappyPathWorld {
    lateinit var restaurants: Array<RestaurantDTO>
    lateinit var selectedRestaurant: RestaurantDTO
    lateinit var dishes: Array<DishDTO>
    lateinit var order: OrderDTO
    lateinit var basket: BasketDTO

    val events = ConcurrentLinkedDeque<Any>()

    val couriers = mutableMapOf<String, CourierClient>()
}