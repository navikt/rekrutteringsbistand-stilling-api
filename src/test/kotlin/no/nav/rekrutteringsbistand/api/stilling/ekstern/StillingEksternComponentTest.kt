package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingForPersonbruker
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class StillingEksternComponentTest {

    @Value("\${vis-stilling.uri}")
    private val uriTilVisStilling: String = "uriTilVisStilling"

    @get:Rule
    val wiremock = WireMockRule(9934)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(8766)

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @Autowired
    lateinit var mockLogin: MockLogin

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {

        val stilling = enStilling

        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)

        val token = mockLogin.hentAzureAdMaskinTilMaskinToken(uriTilVisStilling)

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/ekstern/api/v1/stilling/${stilling.uuid}",
                HttpMethod.GET,
                HttpEntity(null, mapOf(
                        AUTHORIZATION to "Bearer $token"
                ).toMultiValueMap()),
                StillingForPersonbruker::class.java
        ).also {
            assertThat(it.body).isEqualTo(enStillingForPersonbruker)
        }
    }

    fun mockUtenAuthorization(urlPath: String, responseBody: Any) {
        wiremock.stubFor(
                get(urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(responseBody)))
        )
    }
}
