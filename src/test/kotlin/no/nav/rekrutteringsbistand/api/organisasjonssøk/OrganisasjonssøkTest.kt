package no.nav.rekrutteringsbistand.api.organisasjonssøk

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.mockAzureObo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class OrganisasjonssøkTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(9914))

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/veileder-token-cookie", Unit::class.java)
        mockAzureObo(wiremockAzure)
    }

    @Test
    fun `POST mot søk skal videresende HTTP respons body med norske tegn fra pam-ad-api uendret`() {
        mock(HttpMethod.POST, "/search-api/underenhet/_search", organisasjonssøkResponsBody)
        restTemplate.postForEntity("$localBaseUrl/search-api/underenhet/_search", HttpEntity(organisasjonssøkPayload, HttpHeaders()), String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(organisasjonssøkResponsBody)
        }
    }

    @Test
    fun `GET mot søk skal videresende HTTP respons body fra pam-ad-api uendret`() {
        mock(HttpMethod.GET, "/search-api/underenhet/_search\\?q=organisasjonsnummer:([0-9]*)", organisasjonssøkResponsBody)
        restTemplate.getForEntity("$localBaseUrl/search-api/underenhet/_search?q=organisasjonsnummer:123", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(organisasjonssøkResponsBody)
        }
    }

    @Test
    fun `POST mot søk skal videresende HTTP error respons fra pam-ad-api uendret`() {
        mockServerfeil("/search-api/underenhet/_search")
        restTemplate.postForEntity("$localBaseUrl/search-api/underenhet/_search", HttpEntity(organisasjonssøkPayload, HttpHeaders()), String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun mock(method: HttpMethod, urlPath: String, responseBody: String) {
        wiremock.stubFor(
                WireMock.request(method.name, WireMock.urlMatching(urlPath))
                        .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*"))
                        .willReturn(WireMock.aResponse().withStatus(200)
                                .withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(responseBody))
        )
    }

    private fun mockServerfeil(urlPath: String) {
        wiremock.stubFor(
                WireMock.post(WireMock.urlPathMatching(urlPath))
                        .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*"))
                        .willReturn(WireMock.serverError()
                                .withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        ))
    }

    private val organisasjonssøkPayload =
            """
                {"query":{"match_phrase":{"navn_ngram_completion":{"query":"asdfas","slop":5}}},"size":50}
            """.trimIndent()

    private val organisasjonssøkResponsBody =
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
}
