package no.nav.rekrutteringsbistand.api.config

import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestTemplate
import java.net.CookieManager
import java.net.http.HttpClient

/**
 * Kompatibilitetserstatning for Spring Boot sin tidligere TestRestTemplate,
 * som ble fjernet i Spring Boot 4. Arver fra RestTemplate slik at alle
 * kjente metoder (getForEntity, postForEntity, exchange, ...) er tilgjengelige,
 * og slår av feilhåndtering slik at 4xx/5xx ikke kaster exception – akkurat
 * som den opprinnelige TestRestTemplate.
 */
class TestRestTemplate : RestTemplate {

    enum class HttpClientOption { ENABLE_COOKIES }

    constructor() : super() {
        useNonThrowingErrorHandler()
    }

    constructor(vararg options: HttpClientOption) : super() {
        if (options.contains(HttpClientOption.ENABLE_COOKIES)) {
            val httpClient = HttpClient.newBuilder()
                .cookieHandler(CookieManager())
                .build()
            requestFactory = JdkClientHttpRequestFactory(httpClient)
        }
        useNonThrowingErrorHandler()
    }

    val restTemplate: RestTemplate get() = this

    private fun useNonThrowingErrorHandler() {
        errorHandler = object : ResponseErrorHandler {
            override fun hasError(response: ClientHttpResponse): Boolean = false
        }
    }
}
