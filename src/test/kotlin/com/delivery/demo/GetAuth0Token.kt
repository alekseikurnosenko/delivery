package com.delivery.demo

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.client.postForObject

class LoggingRequestInterceptor : ClientHttpRequestInterceptor {

    val logger = LoggerFactory.getLogger(LoggingRequestInterceptor::class.java)

    override fun intercept(request: HttpRequest, body: ByteArray,
                           execution: ClientHttpRequestExecution): ClientHttpResponse {

        logger.info("Method: ", request.getMethod().toString())
        logger.info("URI: ", request.getURI().toString())
        logger.info("Request Body: " + String(body))

        val response = execution.execute(request, body);

        return response
    }


}

@Component
class Auth0Client(
        @Value("\${auth0.client-id}") private val auth0ClientId: String,
        @Value("\${auth0.client-secret}") private val auth0ClientSecret: String,
        @Value("\${auth0.audience}") private val auth0Audience: String
) {
    private val restTemplate: RestTemplate = RestTemplate().apply {
        interceptors.add(LoggingRequestInterceptor())
    }

    fun getToken(audience: String = "https://dev-delivery.auth0.com/api/v2/"): String {
        val headers = HttpHeaders()
        headers.set("content-type", "application/json")
        val entity = HttpEntity(
                "{\"client_id\":\"$auth0ClientId\"," +
                        "\"client_secret\":\"$auth0ClientSecret\"," +
                        "\"audience\":\"$audience\"," +
                        "\"grant_type\":\"client_credentials\"}",
                headers
        )
        val response = restTemplate.exchange<Map<String, String>>(
                "https://dev-delivery.auth0.com/oauth/token",
                HttpMethod.POST,
                entity
        )
        return response.body!!.getValue("access_token")
    }

    fun getUserToken(email: String, password: String): String {
        val payload = "grant_type=password" +
                "&username=$email" +
                "&password=$password" +
                "&audience=$auth0Audience" +
                "&client_id=$auth0ClientId" +
                "&client_secret=$auth0ClientSecret" +
                "&scope="

        val entity = HttpEntity(
                payload,
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                }
        )

        val response = restTemplate.postForObject<Map<String, String>>("https://dev-delivery.auth0.com/oauth/token", entity)

        val token = response["access_token"] ?: throw Exception("Not access_token returned")

        return token
    }

    fun createUser(email: String, password: String) {
        val body = ObjectMapper().writeValueAsString(mapOf(
                "email" to email,
                "password" to password,
                "connection" to "Username-Password-Authentication"))

        val entity = HttpEntity(
                body,
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    setBearerAuth(getToken("https://dev-delivery.auth0.com/api/v2/"))
                }
        )

        try {
            val response = restTemplate.postForObject<Map<String, String>>("https://dev-delivery.auth0.com/api/v2/users", entity)

            println(response)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode != HttpStatus.CONFLICT) {
                throw e
            }
            println("Conflict")
            // User already exists
        }
    }
}