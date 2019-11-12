package no.nav.rekrutteringsbistand.api.konfigurasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.rekrutteringsbistand.api.LOG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

@Profile("mock")
@Configuration
class MockConfig {

    @Bean
    fun wireMockServer(): WireMockServer {
        return WireMockServer(wireMockConfig()
                .notifier(ConsoleNotifier(true))
                .port(9914)).apply {
            stubFor(mappingBuilderStilling())
            stubFor(mappingBuilderSok())
            start()
            LOG.info("Startet WireMock på port ${port()}")
        }
    }

    companion object {
        fun mappingBuilderStilling(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/rekrutteringsbistand/api/v1/ads"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(stillingResponse))

        }

        fun mappingBuilderSok(): MappingBuilder {
            return WireMock.post(WireMock.urlPathMatching("/search-api/underenhet/_search"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(sokResponse))

        }

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

        val sokResponse = """
            {
                "took": 19,
                "timed_out": false,
                "_shards": { "total": 5, "successful": 5, "skipped": 0, "failed": 0 },
                "hits": {
                    "total": 2,
                    "max_score": 17.936558,
                    "hits": [
                        {
                            "_index": "underenhet20190816",
                            "_type": "underenhet",
                            "_id": "976434099",
                            "_score": 17.936558,
                            "_source": {
                                "organisasjonsnummer": "976434099",
                                "navn": "TULLEKONTORET AS",
                                "organisasjonsform": "BEDR",
                                "antallAnsatte": 1,
                                "overordnetEnhet": "912819973",
                                "adresse": {
                                    "adresse": "Lilleakerveien 37D",
                                    "postnummer": "0284",
                                    "poststed": "OSLO",
                                    "kommunenummer": "0301",
                                    "kommune": "OSLO",
                                    "landkode": "NO",
                                    "land": "Norge"
                                },
                                "naringskoder": [
                                    {
                                        "kode": "90.012",
                                        "beskrivelse": "Utovende kunstnere og underholdningsvirksomhet innen scenekunst"
                                    }
                                ]
                            }
                        },
                        {
                            "_index": "underenhet20190816",
                            "_type": "underenhet",
                            "_id": "921730810",
                            "_score": 17.93613,
                            "_source": {
                                "organisasjonsnummer": "921730810",
                                "navn": "TULLEKUNSTNEREN MARTINSEN",
                                "organisasjonsform": "BEDR",
                                "antallAnsatte": 0,
                                "overordnetEnhet": "921323824",
                                "adresse": {
                                    "adresse": "Skytterdalen 7",
                                    "postnummer": "1337",
                                    "poststed": "SANDVIKA",
                                    "kommunenummer": "0219",
                                    "kommune": "ASKER",
                                    "landkode": "NO",
                                    "land": "Norge"
                                },
                                "naringskoder": [{ "kode": "43.341", "beskrivelse": "Malerarbeid" }]
                            }
                        }
                    ]
                }
            }
        """.trimIndent()
    }
}
