package no.nav.rekrutteringsbistand.api.stillingsanalyse

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class StillingsanalyseControllerTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val wiremockAzure: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9954))
            .build()

        @JvmStatic
        @RegisterExtension
        val wiremockOpenAi: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9955))
            .build()
    }

    private val utviklerrolle = "a1749d9a-52e0-4116-bb9f-935c38f6c74a"

    @LocalServerPort
    private var port = 0

    private val lokalBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    @BeforeEach
    fun setUp() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate, listOf(utviklerrolle))
        mockAzureObo(wiremockAzure)
    }

    @Test
    fun `analyserStilling skal kunne kalles og returnere riktig retur`() {
        mockOpenAiResponse(openAiApiResponseBody)

        val stillingsanalyseDto = StillingsanalyseController.StillingsanalyseDto(
            stillingsId = "1",
            stillingstype = Stillingskategori.STILLING,
            stillingstittel = "Teststilling",
            stillingstekst = "Dette er en test",
            source = "DIR"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(stillingsanalyseDto, headers)

        val url = "$lokalBaseUrl/rekrutteringsbistand/stillingsanalyse"

        val response = restTemplate.postForEntity(
            url,
            entity,
            StillingsanalyseController.StillingsanalyseResponsDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isEqualTo(
            StillingsanalyseController.StillingsanalyseResponsDto(
                sensitiv = true,
                sensitivBegrunnelse = "Stillingsbeskrivelsen inneholder sensitive termer.",
                samsvarMedTittel = true,
                tittelBegrunnelse = "Tittel samsvarer ikke",
                samsvarMedType = true,
                typeBegrunnelse = "Type samsvarer ikke"
            )
        )
    }

    @Test
    fun `analyserStilling skal returnere bad request hvis source ikke er DIR`() {
        mockOpenAiResponse(openAiApiResponseBody)

        val stillingsanalyseDto = StillingsanalyseController.StillingsanalyseDto(
            stillingsId = "7",
            stillingstype = Stillingskategori.STILLING,
            stillingstittel = "Teststilling 12345678",
            stillingstekst = "Dette er en test med telefonnummer 87654321 og e-post test@eksempel.no.",
            source = "ASS"
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(stillingsanalyseDto, headers)
        val url = "$lokalBaseUrl/rekrutteringsbistand/stillingsanalyse"

        val response = restTemplate.postForEntity(
            url, entity, String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        wiremockOpenAi.verify(
            0, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview")
            )
        )
    }

    @Test
    fun `analyserStilling skal returnere forbidden hvis bruker mangler rolle`() {
        mockOpenAiResponse(openAiApiResponseBody)

        val stillingsanalyseDto = StillingsanalyseController.StillingsanalyseDto(
            stillingsId = "8",
            stillingstype = Stillingskategori.STILLING,
            stillingstittel = "Ingeniør 12345678",
            stillingstekst = "Kontakt oss på 12345678 eller send en e-post til ingen@firma.no.",
            source = "DIR"
        )

        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        val entity = HttpEntity(stillingsanalyseDto, headers)
        val url = "$lokalBaseUrl/rekrutteringsbistand/stillingsanalyse"

        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)

        val response = restTemplate.postForEntity(
            url, entity, String::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)

        wiremockOpenAi.verify(
            0, WireMock.postRequestedFor(
                WireMock.urlEqualTo("/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview")
            )
        )
    }

    private fun mockOpenAiResponse(responseBody: String) {
        wiremockOpenAi.stubFor(
            WireMock.post(WireMock.urlEqualTo("/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview"))
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).withBody(responseBody)
                )
        )
    }

    private val openAiApiResponseBody = """
       {
          "choices": [
            {
              "message": {
                "content": "{ \"sensitiv\": true, \"sensitivBegrunnelse\": \"Stillingsbeskrivelsen inneholder sensitive termer.\", \"samsvarMedTittel\": true, \"tittelBegrunnelse\": \"Tittel samsvarer ikke\", \"samsvarMedType\": true, \"typeBegrunnelse\": \"Type samsvarer ikke\" }"
              }
            }
          ]
        }
    """.trimIndent()
}
