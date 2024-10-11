package no.nav.rekrutteringsbistand.api.stillingsanalyse

import OpenAiClient
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.web.client.RestTemplate

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class StillingsanalyseControllerTest {

    @TestConfiguration
    class TestConfig {
        @Bean
        fun openAiClient(): OpenAiClient {
            // Return a mock or a real instance as needed
            return OpenAiClient(
                restTemplate = RestTemplate(),
                openAiApiUrl = "http://localhost:9955/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview", // Use a test URL
                openAiApiKey = "test-key"
            )
        }
    }

    val utviklerrolle = "a1749d9a-52e0-4116-bb9f-935c38f6c74a"

    @get:Rule
    val wiremockOpenAi = WireMockRule(WireMockConfiguration.options().port(9955))

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @LocalServerPort
    private var port = 0

    private val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    @Before
    fun setUp() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate, listOf(utviklerrolle))
        mockAzureObo(wiremockAzure)

    }

    @Test
    fun `analyserStilling should return correct response`() {
        mockOpenAiResponse(openAiApiResponseBody)

        val stillingsanalyseDto = StillingsanalyseController.StillingsanalyseDto(
            stillingsId = "1",
            stillingstype = Stillingskategori.STILLING,
            stillingstittel = "Teststilling",
            stillingstekst = "Dette er en test"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(stillingsanalyseDto, headers)

        val url = "$localBaseUrl/rekrutteringsbistand/stillingsanalyse"

        val response = restTemplate.postForEntity(
            url,
            entity,
            StillingsanalyseController.StillingsanalyseResponsDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(
            StillingsanalyseController.StillingsanalyseResponsDto(
                sensitiv = true,
                begrunnelse = "Stillingsbeskrivelsen inneholder sensitive termer."
            )
        )
    }

    @Test
    fun `analyserStilling should return 403 Forbidden if user lacks UTVIKLER role`() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)

        val stillingsanalyseDto = StillingsanalyseController.StillingsanalyseDto(
            stillingsId = "1",
            stillingstype = Stillingskategori.STILLING,
            stillingstittel = "Teststilling",
            stillingstekst = "Dette er en test"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(stillingsanalyseDto, headers)
        val url = "$localBaseUrl/rekrutteringsbistand/stillingsanalyse"
        val response = restTemplate.postForEntity(url, entity, String::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
    }

    private fun mockOpenAiResponse(responseBody: String) {
        wiremockOpenAi.stubFor(
            WireMock.post(WireMock.urlEqualTo("/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)
                )
        )
    }

    // Mocked response from OpenAI API
    private val openAiApiResponseBody = """
        {
            "choices": [
                {
                    "message": {
                        "content": "{ \"sensitiv\": true, \"begrunnelse\": \"Stillingsbeskrivelsen inneholder sensitive termer.\" }"
                    }
                }
            ]
        }
    """.trimIndent()
}
