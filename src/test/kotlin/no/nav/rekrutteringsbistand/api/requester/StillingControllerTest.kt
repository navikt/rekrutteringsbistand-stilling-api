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
    fun hentStillingerReturnererStillinger() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/ads?a=a", String::class.java).apply {
            Assertions.assertThat(this)
                    .isEqualToIgnoringWhitespace(stillingResponse)
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
            return WireMock.get(WireMock.urlPathMatching("/rekrutteringsbistand/api/v1/ads"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
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


        val sokResponse = """
            {
                "took":152
            }
        """.trimIndent()

        val stillingResponse = """
            {
                "content": [
                    {
                        "id": 1000,
                        "uuid": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
                        "created": "2019-11-11T14:58:22.815329",
                        "createdBy": "nss-admin",
                        "updated": "2019-11-11T15:18:37.218633",
                        "updatedBy": "nss-admin",
                        "title": "testnss",
                        "status": "ACTIVE",
                        "privacy": "SHOW_ALL",
                        "source": "ASS",
                        "medium": "ASS",
                        "reference": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
                        "published": "2019-11-11T15:01:30.940226",
                        "expires": "2019-11-12T02:00:00",
                        "employer": {
                            "name": "NES & NES AS",
                            "orgnr": "914163854",
                            "status": "ACTIVE",
                            "parentOrgnr": "914134390",
                            "publicName": "NES & NES AS",
                            "deactivated": null
                        },
                        "administration": {
                            "status": "DONE",
                            "comments": null,
                            "reportee": "Clark Kent",
                            "remarks": [],
                            "navIdent": "C12345"
                        },
                        "location": {
                            "postalCode": null,
                            "county": "OSLO",
                            "municipal": "OSLO",
                            "municipalCode": "0301",
                            "city": null,
                            "country": "NORGE"
                        },
                        "locationList": [
                            {
                                "postalCode": null,
                                "county": "OSLO",
                                "municipal": "OSLO",
                                "municipalCode": "0301",
                                "city": null,
                                "country": "NORGE"
                            }
                        ],
                        "categoryList": [
                            {
                                "code": "0000.01",
                                "categoryType": "STYRK08NAV",
                                "name": "Hjelpearbeider (privat/offentlig virksomhet)"
                            }
                        ],
                        "properties": {
                            "extent": "Heltid",
                            "workhours": "[\"Dagtid\"]",
                            "workday": "[\"Ukedager\"]",
                            "applicationdue": "10102020",
                            "jobtitle": "ggg",
                            "searchtags": "[{\"label\":\"Siviltjenestearbeider\",\"score\":1.0},{\"label\":\"Miljoarbeider sosiale fagfelt\",\"score\":0.07061906}]",
                            "positioncount": "1",
                            "engagementtype": "Sesong",
                            "jobarrangement": "Skift",
                            "classification_input_source": "categoryName",
                            "sector": "Privat",
                            "adtext": "<p>test </p>"
                        },
                        "rekruttering": null
                    }
                ],
                "totalPages": 1,
                "totalElements": 1
            }

        """.trimIndent()
    }
}
