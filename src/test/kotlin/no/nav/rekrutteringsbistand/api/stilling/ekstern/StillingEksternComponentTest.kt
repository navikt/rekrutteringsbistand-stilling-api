package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
internal class StillingEksternComponentTest {

    @get:Rule
    val wiremock = WireMockRule(9914)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(9924)

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)
        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/ekstern/api/v1/stilling/${enStilling.uuid}", Stilling
        ::class.java).also {
            assertThat(it).isEqualTo(enEksternStilling)
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

    val enEksternStilling = Stilling(
            id = enStilling.id,
            updated = enStilling.updated,
            title = enStilling.title,
            medium = enStilling.medium,
            employer = null,
            location = enStilling.location
    )
}
