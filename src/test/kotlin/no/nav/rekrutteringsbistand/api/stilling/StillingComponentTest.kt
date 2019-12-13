package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enPage
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingUtenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.http.HttpHeaders.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
internal class StillingComponentTest {

    @get:Rule
    val wiremock = WireMockRule(options().port(9914))

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/local/cookie-isso", String::class.java)
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)
        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/v1/stilling/${enStilling.uuid}", StillingMedStillingsinfo::class.java).also {
            assertThat(it).isEqualTo(enStilling)
        }
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling beriket med stillingsinfo`() {
        repository.lagre(enStillingsinfo)

        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/v1/stilling/${enStilling.uuid}", StillingMedStillingsinfo::class.java).also {
            assertThat(it.rekruttering).isEqualTo(enStillingsinfo.asDto())
            assertThat(it.uuid).isEqualTo(enStillingsinfo.stillingsid.asString())
        }
    }

    @Test
    fun `POST mot søk skal videresende HTTP respons body med norske tegn fra pam-ad-api uendret`() {
        val stillingsSokResponsMedNorskeBokstaver =
                """
                    {
                        "took": 52,
                        "timed_out": false,
                        "_shards": { "total": 3, "successful": 3, "skipped": 0, "failed": 0 },
                        "hits": {
                            "total": { "value": 2182, "relation": "eq" },
                            "max_score": 10.240799,
                            "hits": [
                                {
                                    "_index": "underenhet20191204",
                                    "_type": "_doc",
                                    "_id": "914163854",
                                    "_score": 10.240799,
                                    "_source": {
                                        "organisasjonsnummer": "914163854",
                                        "navn": "NÆS & NÅS AS",
                                        "organisasjonsform": "BEDR",
                                        "antallAnsatte": 6,
                                        "overordnetEnhet": "914134390",
                                        "adresse": {
                                            "adresse": "Klasatjønnveien 30",
                                            "postnummer": "5172",
                                            "poststed": "LODDEFJORD",
                                            "kommunenummer": "1201",
                                            "kommune": "BERGEN",
                                            "landkode": "NO",
                                            "land": "Norge"
                                        },
                                        "naringskoder": [
                                            {
                                                "kode": "41.200",
                                                "beskrivelse": "Oppføring av bygninger"
                                            }
                                        ]
                                    }
                                }
                            ]
                        }
                    }
                """.trimIndent()
        mockString("/search-api/underenhet/_search", stillingsSokResponsMedNorskeBokstaver)
        restTemplate.postForObject("$localBaseUrl/search-api/underenhet/_search", HttpEntity("{}", HttpHeaders()), String::class.java).also {
            assertThat(it).isEqualTo(stillingsSokResponsMedNorskeBokstaver)
        }
    }

    @Test
    fun `POST mot søk skal videresende HTTP error respons fra pam-ad-api uendret`() {
        mockServerfeil("/search-api/underenhet/_search")
        restTemplate.exchange("$localBaseUrl/search-api/underenhet/_search", HttpMethod.POST, HttpEntity("{}", HttpHeaders()), String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    @Test
    fun `GET mot stillinger skal returnere stillinger beriket med stillingsinfo`() {
        repository.lagre(enStillingsinfo)
        repository.lagre(enAnnenStillingsinfo)

        mock(HttpMethod.GET, "/rekrutteringsbistand/api/v1/ads", enPage)

        val stillinger: List<StillingMedStillingsinfo> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        ).body!!.content

        assertThat(stillinger.first().rekruttering).isEqualTo(enStillingsinfo.asDto())
        assertThat(stillinger.last().rekruttering).isEqualTo(enAnnenStillingsinfo.asDto())
    }

    @Test
    fun `POST mot stillinger skal returnere stilling`() {

        mock(HttpMethod.POST, "/rekrutteringsbistand/api/v1/ads", enStilling)

        restTemplate.postForObject(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads",
                enStilling.copy(uuid = null),
                StillingMedStillingsinfo::class.java
        ).also {
            assertThat(it.uuid).isNotEmpty()
            assertThat(it.copy(uuid = null)).isEqualTo(enStilling.copy(uuid = null))
        }
    }

    @Test
    fun `PUT mot stilling skal returnere endret stilling`() {
        mock(HttpMethod.PUT, "/rekrutteringsbistand/api/v1/ads/${enStilling.uuid}", enStilling)

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${enStilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(enStilling.copy(uuid = null)),
                StillingMedStillingsinfo::class.java
        ).body.also {
            assertThat(it!!.uuid).isNotEmpty()
            assertThat(it.copy(uuid = null)).isEqualTo(enStilling.copy(uuid = null))
        }
    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger uten stillingsinfo`() {
        mock(HttpMethod.GET, "/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger", enPage)

        val respons: ResponseEntity<Page<StillingMedStillingsinfo>> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                HttpMethod.GET,
                HttpEntity("{}", HttpHeaders()),
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isEqualTo(enPage)
    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger med stillingsinfo`() {
        repository.lagre(enStillingsinfo)
        repository.lagre(enAnnenStillingsinfo)

        mock(HttpMethod.GET, "/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger", enPage)

        val respons: ResponseEntity<Page<StillingMedStillingsinfo>> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                HttpMethod.GET,
                HttpEntity("{}", HttpHeaders()),
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.content.first().rekruttering).isEqualTo(enStillingsinfo.asDto())
        assertThat(respons.body!!.content.last().rekruttering).isEqualTo(enAnnenStillingsinfo.asDto())
    }

    @Test
    fun `DELETE mot stilling skal returnere HTTP 200 med stilling og status DELETED`() {
        val slettetStilling = enStillingUtenStillingsinfo.copy(status = "DELETED")
        mock(HttpMethod.DELETE, "/rekrutteringsbistand/api/v1/ads/${slettetStilling.uuid}", slettetStilling)

        val respons: ResponseEntity<Stilling> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${slettetStilling.uuid}",
                HttpMethod.DELETE,
                HttpEntity("{}", HttpHeaders()),
                Stilling::class.java
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isEqualTo(slettetStilling)
    }

    private fun mock(method: HttpMethod, urlPath: String, body: Any) {
        stubFor(
                request(method.name, urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(AUTHORIZATION, matching("Bearer .*}"))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(body)))
        )
    }

    private fun mockUtenAuthorization(urlPath: String, body: Any) {
        stubFor(
                get(urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(body)))
        )
    }

    private fun mockString(urlPath: String, body: String) {
        stubFor(
                post(urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(AUTHORIZATION, matching("Bearer .*}"))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(body))
        )
    }

    private fun mockServerfeil(urlPath: String) {
        stubFor(
                post(urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(AUTHORIZATION, matching("Bearer .*}"))
                        .willReturn(serverError()
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        ))
    }

    @After
    fun tearDown() {
        repository.slett(enStillingsinfo.stillingsinfoid)
        repository.slett(enAnnenStillingsinfo.stillingsinfoid)
    }
}
