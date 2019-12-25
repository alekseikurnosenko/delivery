//package com.delivery.demo
//
//import com.delivery.demo.basket.AddToBasketInput
//import com.delivery.demo.basket.Basket
//import com.delivery.demo.restaurant.Restaurant
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.module.kotlin.readValue
//import org.aspectj.lang.annotation.Before
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.fail
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters
//import org.springframework.boot.test.autoconfigure.json.JsonTest
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.test.context.ContextConfiguration
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.get
//import org.springframework.boot.test.web.client.TestRestTemplate
//import org.springframework.boot.test.web.client.getForObject
//import org.springframework.boot.test.web.client.postForObject
//import org.springframework.boot.web.server.LocalServerPort
//import org.springframework.http.client.ClientHttpRequestInterceptor
//
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//class DemoApplicationTests {
//
//    val TOKEN =
//        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlJVUkNNa0ZHTWpsR1FUZ3lPVU5CUkVFMU9EaEJNRGt3UlRaRVEwRkZPVU0xUkRVelF6aERRZyJ9.eyJpc3MiOiJodHRwczovL2Rldi1kZWxpdmVyeS5hdXRoMC5jb20vIiwic3ViIjoiR1l1OHFydUpoTnpMTTFKZWlQaWNVWFpmSXljNjNlUXZAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vZGVsaXZlcnkvYXBpIiwiaWF0IjoxNTc2MzYxMjE2LCJleHAiOjE1NzY0NDc2MTYsImF6cCI6IkdZdThxcnVKaE56TE0xSmVpUGljVVhaZkl5YzYzZVF2IiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.t4AMvz2BKz6oFidQtstSWrlSRNnwqgrJrtZuxpwxb5y4H8-JVJvzpiCxfis9ZNua5D5eE_BVkbkHXXDP18Idad-MDBLiNSDGQL7nXRXy1rm_ddZMP1amkrVgj3TYfHu5c0TDzK10ob-USIswRulpIqyAV4c5qvsumSBAmO3_oDkQIyjGUXRFQNhNWhq59ePzFzYJAXFegeM_iwy36jUWUn4ET__WtQ3IkKGqUfzg96DXKVezdwvryKbXTNbdlDduIqaHXFRcB0YmkldJLi-70nR5otKnc0cc7kHL_7Z99ssfVEm6XtNaHlrVO1MxUPQfeh_LNHPSdtdIgGfEb7NwSQ"
//
//    @LocalServerPort
//    var port: Int = 0
//
//    @Autowired
//    lateinit var mapper: ObjectMapper
//
//    @Autowired
//    lateinit var restTemplate: TestRestTemplate
//
//    @BeforeEach
//    fun before() {
//        restTemplate.restTemplate.interceptors = listOf(
//            ClientHttpRequestInterceptor { request, body, execution ->
//                request.headers.add("Authorization", "Bearer $TOKEN")
//                return@ClientHttpRequestInterceptor execution.execute(request, body)
//            }
//        )
//    }
//
//    @Test
//    fun happyPath() {
//        println("-------------------->")
//        val restaurants = restTemplate.getForObject<String>(api("/api/restaurants"))
//        println(restaurants)
//
////
////        val basket = restTemplate.getForObject<String>(api("/api/basket"))
////        println(basket)
////
////        val selectedRestaurant = restaurants!!.first()
////        val dish = selectedRestaurant.dishes.first()
////        println(restTemplate.postForObject<String>(api("/api/addItem"), AddToBasketInput(dish.id, selectedRestaurant.id, 1)))
//
//
//
//        println("-------------------->")
//    }
//
//    fun api(path: String): String = "http://localhost:$port$path"
//
//    private inline fun <reified T> String?.parseAs(): T? {
//        return this?.let { mapper.readValue<T>(it) }
//    }
//}