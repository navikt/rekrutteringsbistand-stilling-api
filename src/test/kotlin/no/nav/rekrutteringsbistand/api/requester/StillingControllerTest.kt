package no.nav.rekrutteringsbistand.api.requester

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mvc-test")
internal class StillingControllerTest {

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("${localBaseUrl()}/local/cookie-isso", String::class.java)
    }

    @LocalServerPort
    var port = 0

    private fun localBaseUrl(): String = "http://localhost:$port/rekrutteringsbistand-api"

    @Test
    fun hentStillingReturnererStilling() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/ads", String::class.java).apply {
            Assertions.assertThat(this)
                    .isEqualTo(stillingResponse)
        }

    }

    @Test
    fun hentStillingReturnererSok() {
        val headers = HttpHeaders()

        val request = HttpEntity("body", headers)
        restTemplate.postForObject("${localBaseUrl()}/search-api/underenhet/_search", request, String::class.java).apply {
            Assertions.assertThat(this)
                    .isEqualTo(sokResponse)
        }

    }

    companion object {
        fun mappingBuilderStilling(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/ads"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withBody(stillingResponse))

        }

        fun mappingBuilderSok(): MappingBuilder {
            return WireMock.post(WireMock.urlPathMatching("/search-api/underenhet/_search"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*}"))
                    .withRequestBody(equalTo("body"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withBody(sokResponse))

        }

        val stillingResponse = """
            {
              "content": [],
            }
        """.trimIndent()

        val sokResponse = """
            {
                "took":152
            }
        """.trimIndent()
    }
}
