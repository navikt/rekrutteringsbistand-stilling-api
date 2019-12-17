package no.nav.rekrutteringsbistand.api.support.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

@Profile("sokMock")
@Configuration
class SokMockConfig {

    @Bean(name = ["SokWireMock"])
    fun wireMockServer(): WireMockServer {
        return WireMockServer(wireMockConfig()
                .notifier(ConsoleNotifier(true))
                .port(9934)).apply {
            stubFor(mappingBuilderSok())
            start()
            LOG.info("Startet WireMock på port ${port()}")
        }
    }

    companion object {

        fun mappingBuilderSok(): MappingBuilder {
            return WireMock.any(WireMock.urlPathMatching("/search-api/underenhet/_search"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(sokResponse))
        }

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
