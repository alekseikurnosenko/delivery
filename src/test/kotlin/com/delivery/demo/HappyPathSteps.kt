package com.delivery.demo

import com.delivery.restaurant.RestaurantDTO
import io.cucumber.java8.En
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity
import org.springframework.web.client.getForObject

class HappyPathSteps : En {

    private val restTemplate = RestTemplate()

    val TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6IlJVUkNNa0ZHTWpsR1FUZ3lPVU5CUkVFMU9EaEJNRGt3UlRaRVEwRkZPVU0xUkRVelF6aERRZyJ9.eyJpc3MiOiJodHRwczovL2Rldi1kZWxpdmVyeS5hdXRoMC5jb20vIiwic3ViIjoiR1l1OHFydUpoTnpMTTFKZWlQaWNVWFpmSXljNjNlUXZAY2xpZW50cyIsImF1ZCI6Imh0dHBzOi8vZGVsaXZlcnkvYXBpIiwiaWF0IjoxNTc3MDM4MjAyLCJleHAiOjE1NzcxMjQ2MDIsImF6cCI6IkdZdThxcnVKaE56TE0xSmVpUGljVVhaZkl5YzYzZVF2IiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.S96iI5AlwIlP3I4a9Hx0nz7x4TnzdRKW7uUtuIkJU7fWps6qkG7MA5ZOfWW1V6ZpH5IX-dHolSs59iBTD1nqm7dzIGkTnubx59hW03OMF-fy9QOu9fB2SiBK_fKcIR3ZOGiPRWEfJDafQek3MxSosxoGOAGDzjEwlPNg3CpYzIG-b1qX2v5JU-CeUUqY_drO_RRu5DS24DejVrYxWeCPI1u0CD5khTbjQc0kDph_o0IIIIzM5-LykzIe44IEBJtwElMxKSDDmuvCN1xdXwOyjshJ0ysc6qHN9rpDlwtSg1vWSyZg8mShVgbxYvjNci8vivM8vvthCzAHhdhBEVldpg"


    @LocalServerPort
    var serverPort = 0

    init {
        Given("A signed-in user") {
            restTemplate.interceptors = listOf(
                ClientHttpRequestInterceptor { request, body, execution ->
                    request.headers.add("Authorization", "Bearer $TOKEN")
                    return@ClientHttpRequestInterceptor execution.execute(request, body)
                }
            )
        }
        And("user's basket is empty") {

        }
        When("user browses list of available restaurants") {
            val response =
                restTemplate.getForObject<List<RestaurantDTO>>("http://localhost:$serverPort/api/restaurants")
            println(response)
        }
    }
}