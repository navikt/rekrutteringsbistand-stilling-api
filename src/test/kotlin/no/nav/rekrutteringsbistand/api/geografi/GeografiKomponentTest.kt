package no.nav.rekrutteringsbistand.api.geografi

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
internal class GeografiKomponentTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(9914))

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/local/cookie-isso", String::class.java)
    }

    @Test
    fun `Skal kunne hente fylker`() {
        val fylkerespons =
                """
                   [
                     {
                       "code": "02",
                       "name": "AKERSHUS"
                     },
                     {
                       "code": "03",
                       "name": "OSLO"
                     },
                     {
                       "code": "09",
                       "name": "AUST-AGDER"
                     },
                     {
                       "code": "06",
                       "name": "BUSKERUD"
                     },
                     {
                       "code": "08",
                       "name": "TELEMARK"
                     },
                     {
                       "code": "23",
                       "name": "KONTINENTALSOKKELEN"
                     },
                     {
                       "code": "05",
                       "name": "OPPLAND"
                     },
                     {
                       "code": "15",
                       "name": "MØRE OG ROMSDAL"
                     },
                     {
                       "code": "11",
                       "name": "ROGALAND"
                     },
                     {
                       "code": "12",
                       "name": "HORDALAND"
                     },
                     {
                       "code": "18",
                       "name": "NORDLAND"
                     },
                     {
                       "code": "20",
                       "name": "FINNMARK"
                     },
                     {
                       "code": "14",
                       "name": "SOGN OG FJORDANE"
                     },
                     {
                       "code": "21",
                       "name": "SVALBARD"
                     },
                     {
                       "code": "04",
                       "name": "HEDMARK"
                     },
                     {
                       "code": "50",
                       "name": "TRØNDELAG"
                     },
                     {
                       "code": "22",
                       "name": "JAN MAYEN"
                     },
                     {
                       "code": "19",
                       "name": "TROMS"
                     },
                     {
                       "code": "10",
                       "name": "VEST-AGDER"
                     },
                     {
                       "code": "07",
                       "name": "VESTFOLD"
                     },
                     {
                       "code": "01",
                       "name": "ØSTFOLD"
                     }
                   ]
                """.trimIndent()
        mockString("/rekrutteringsbistand/api/v1/geography/counties", fylkerespons)
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/geography/counties", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(fylkerespons)
        }
    }

    @Test
    fun `GET mot municipals skal returnere HTTP 200 med kommuner`() {
        mockString("/rekrutteringsbistand/api/v1/geography/municipals", kommunerJson);
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/geography/municipals", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(kommunerJson)
        };
    }

    @Test
    fun `GET mot categories-with-altnames skal returnere HTTP 200 med STYRK-kategorier`() {
        mockString("/rekrutteringsbistand/api/v1/categories-with-altnames", styrkkoderJson);
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/categories-with-altnames", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(styrkkoderJson)
        }
    }

    @Test
    fun `GET mot postdata skal returnere HTTP 200 med informasjon om postnumre`() {
        mockString("/rekrutteringsbistand/api/v1/postdata", postnumreJson);
        restTemplate.getForEntity("$localBaseUrl/rekrutteringsbistand/api/v1/postdata", String::class.java).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(it.body).isEqualTo(postnumreJson)
        }
    }

    private fun mockString(urlPath: String, body: String) {
        wiremock.stubFor(
                WireMock.get(WireMock.urlPathMatching(urlPath))
                        .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                        .willReturn(WireMock.aResponse().withStatus(200)
                                .withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(body))
        )
    }

    private val kommunerJson = """
            [
                {
                    "code": "1818",
                    "name": "HERØY (NORDLAND)",
                    "countyCode": "18"
                },
                {
                    "code": "1903",
                    "name": "HARSTAD",
                    "countyCode": "19"
                }
            ]
        """.trimIndent()

    private val styrkkoderJson = """
            [
                {
                    "id": 393,
                    "code": "1311.21",
                    "categoryType": "STYRK08NAV",
                    "name": "Fylkesgartner",
                    "description": null,
                    "parentId": 372,
                    "alternativeNames": []
                }
            ]
        """.trimIndent()

    private val postnumreJson = """
            [
                {
                    "postalCode": "4971",
                    "city": "SUNDEBRU",
                    "municipality": {
                        "code": "0911",
                        "name": "GJERSTAD",
                        "countyCode": "09"
                    },
                    "county": {
                        "code": "09",
                        "name": "AUST-AGDER"
                    }
                }
            ]
        """.trimIndent()
}

