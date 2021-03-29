package no.nav.rekrutteringsbistand.api.config

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

@Profile("stillingMock")
@Configuration
class MockConfig {

    @Bean(name = ["StillingWireMock"])
    fun wireMockServer(): WireMockServer {
        return WireMockServer(wireMockConfig()
                .notifier(ConsoleNotifier(true))
                .port(9914)).apply {
            stubFor(hentStillinger())
            stubFor(hentStilling())
            stubFor(postStilling())
            stubFor(putStilling())
            stubFor(categoriesTypeahead())
            stubFor(postdata())
            stubFor(municipals())
            stubFor(countries())
            stubFor(counties())
            start()
            LOG.info("Startet WireMock på port ${port()}")
        }
    }

    companion object {
        fun hentStillinger(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/ads"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(stillingerResponse))
        }

        fun hentStilling(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/ads/ee82f29c-51a9-4ca3-994d-45e3ab0e8204"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(stillingResponse))
        }

        fun postStilling(): MappingBuilder {
            return WireMock.post(WireMock.urlPathMatching("/api/v1/ads"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(201)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(postStillingResponse))
        }

        fun putStilling(): MappingBuilder {
            return WireMock.put(WireMock.urlPathMatching("/api/v1/ads/ee82f29c-51a9-4ca3-994d-45e3ab0e8204"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(postStillingResponse))
        }

        fun categoriesTypeahead(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/categories-with-altnames/"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(categoriesTypeaheadResponse))
        }

        fun postdata(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/postdata/"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(postdata))
        }

        fun municipals(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/geography/municipals"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(municipalsResponse))
        }

        fun countries(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/geography/countries"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(countriesResponse))
        }

        fun counties(): MappingBuilder {
            return WireMock.get(WireMock.urlPathMatching("/api/v1/geography/counties"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                    .withHeader(HttpHeaders.AUTHORIZATION, WireMock.matching("Bearer .*}"))
                    .willReturn(WireMock.aResponse().withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(countiesResponse))
        }

        private val stillingerResponse = """
            {
            "content": [
                {
                    "id": 1000,
                    "uuid": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
                    "created": "2019-11-11T14:58:22.815329",
                    "createdBy": "nss-admin",
                    "updated": "2019-11-11T15:18:37.218633",
                    "updatedBy": "nss-admin",
                    "mediaList": [],
                    "contactList": [],
                    "title": "testnss",
                    "status": "ACTIVE",
                    "privacy": "SHOW_ALL",
                    "source": "ASS",
                    "medium": "ASS",
                    "reference": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
                    "published": "2019-11-11T15:01:30.940226",
                    "expires": "2019-11-12T02:00:00",
                    "employer": {
                        "id": null,
                        "uuid": null,
                        "created": null,
                        "createdBy": null,
                        "updated": null,
                        "updatedBy": null,
                        "mediaList": [],
                        "contactList": [],
                        "location": null,
                        "locationList": [],
                        "properties": {},
                        "name": "NES & NES AS",
                        "orgnr": "914163854",
                        "status": "ACTIVE",
                        "parentOrgnr": "914134390",
                        "publicName": "NES & NES AS",
                        "deactivated": null,
                        "orgform": null,
                        "employees": null
                    },
                    "administration": {
                        "id": null,
                        "status": "DONE",
                        "comments": null,
                        "reportee": "Clark Kent",
                        "remarks": [],
                        "navIdent": "C12345"
                    },
                    "location": {
                        "address": null,
                        "postalCode": null,
                        "county": "OSLO",
                        "municipal": "OSLO",
                        "municipalCode": "0301",
                        "city": null,
                        "country": "NORGE",
                        "latitude": null,
                        "longitude": null
                    },
                    "locationList": [
                        {
                            "address": null,
                            "postalCode": null,
                            "county": "OSLO",
                            "municipal": "OSLO",
                            "municipalCode": "0301",
                            "city": null,
                            "country": "NORGE",
                            "latitude": null,
                            "longitude": null
                        }
                    ],
                    "categoryList": [
                        {
                            "id": null,
                            "code": "0000.01",
                            "categoryType": "STYRK08NAV",
                            "name": "Hjelpearbeider (privat/offentlig virksomhet)",
                            "description": null,
                            "parentId": null
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
                    "publishedByAdmin": null,
                    "businessName": null,
                    "firstPublished": null,
                    "deactivatedByExpiry": null,
                    "activationOnPublishingDate": null,
                    "rekruttering": null
                }
            ],
            "totalPages": 1,
            "totalElements": 1
        }
        """.trimIndent()

        private val stillingResponse = """
               {
            "id": 1000,
            "uuid": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
            "created": "2019-11-11T14:58:22.815329",
            "createdBy": "nss-admin",
            "updated": "2019-11-11T15:18:37.218633",
            "updatedBy": "nss-admin",
            "mediaList": [],
            "contactList": [],
            "title": "testnss",
            "status": "ACTIVE",
            "privacy": "SHOW_ALL",
            "source": "ASS",
            "medium": "ASS",
            "reference": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
            "published": "2019-11-11T15:01:30.940226",
            "expires": "2019-11-12T02:00:00",
            "employer": {
                "id": null,
                "uuid": null,
                "created": null,
                "createdBy": null,
                "updated": null,
                "updatedBy": null,
                "mediaList": [],
                "contactList": [],
                "location": null,
                "locationList": [],
                "properties": {},
                "name": "NES & NES AS",
                "orgnr": "914163854",
                "status": "ACTIVE",
                "parentOrgnr": "914134390",
                "publicName": "NES & NES AS",
                "deactivated": null,
                "orgform": null,
                "employees": null
            },
            "administration": {
                "id": null,
                "status": "DONE",
                "comments": null,
                "reportee": "Clark Kent",
                "remarks": [],
                "navIdent": "C12345"
            },
            "location": {
                "address": null,
                "postalCode": null,
                "county": "OSLO",
                "municipal": "OSLO",
                "municipalCode": "0301",
                "city": null,
                "country": "NORGE",
                "latitude": null,
                "longitude": null
            },
            "locationList": [
                {
                    "address": null,
                    "postalCode": null,
                    "county": "OSLO",
                    "municipal": "OSLO",
                    "municipalCode": "0301",
                    "city": null,
                    "country": "NORGE",
                    "latitude": null,
                    "longitude": null
                }
            ],
            "categoryList": [
                {
                    "id": null,
                    "code": "0000.01",
                    "categoryType": "STYRK08NAV",
                    "name": "Hjelpearbeider (privat/offentlig virksomhet)",
                    "description": null,
                    "parentId": null
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
            "publishedByAdmin": null,
            "businessName": null,
            "firstPublished": null,
            "deactivatedByExpiry": null,
            "activationOnPublishingDate": null,
            "rekruttering": null
        }

        """.trimIndent()

        private val postStillingResponse = """
            {
               "id": 1000,
               "uuid": "ee82f29c-51a9-4ca3-994d-45e3ab0e8204",
               "created": "2019-11-12T14:17:41.105594",
               "createdBy": "pam-rekrutteringsbistand",
               "updated": "2019-11-12T14:17:41.105594",
               "updatedBy": "pam-rekrutteringsbistand",
               "mediaList": [

               ],
               "contactList":[

               ],
               "location":null,
               "locationList":[

               ],
               "properties":{

               },
               "title":"Ny stilling",
               "status":"INACTIVE",
               "privacy":"INTERNAL_NOT_SHOWN",
               "source":"DIR",
               "medium":"DIR",
               "reference":"398d532f-3ca1-4ca5-bc9b-da9a4945a510",
               "published":"2019-11-12T14:17:41.092249",
               "expires":"2019-11-12T14:17:41.092272",
               "employer":null,
               "categoryList":[

               ],
               "administration":{
                  "id":1000,
                  "status":"PENDING",
                  "comments":null,
                  "reportee":"Clark Kent",
                  "remarks":[

                  ],
                  "navIdent":"C12345"
               },
               "publishedByAdmin":null,
               "businessName":null,
               "firstPublished":false,
               "deactivatedByExpiry":false,
               "activationOnPublishingDate":false
            }
        """.trimIndent()

        private val categoriesTypeaheadResponse = """
            [
               {
                  "id":1,
                  "code":"0",
                  "categoryType":"STYRK08NAV",
                  "name":" MILITÆRE YRKER OG UOPPGITT",
                  "description":null,
                  "parentId":null,
                  "alternativeNames":[

                  ]
               },
               {
                  "id":2,
                  "code":"00",
                  "categoryType":"STYRK08NAV",
                  "name":"Uoppgitt eller yrker som ikke kan identifiseres",
                  "description":null,
                  "parentId":1,
                  "alternativeNames":[

                  ]
               }
            ]
        """.trimIndent()

        private val postdata = """
            [
               {
                  "postalCode":"3656",
                  "city":"ATRÅ",
                  "municipality":{
                     "code":"0826",
                     "name":"TINN",
                     "countyCode":"08"
                  },
                  "county":{
                     "code":"08",
                     "name":"TELEMARK"
                  }
               },
               {
                  "postalCode":"3658",
                  "city":"MILAND",
                  "municipality":{
                     "code":"0826",
                     "name":"TINN",
                     "countyCode":"08"
                  },
                  "county":{
                     "code":"08",
                     "name":"TELEMARK"
                  }
               },
               {
                  "postalCode":"2329",
                  "city":"VANG PÅ HEDMARKEN",
                  "municipality":{
                     "code":"0403",
                     "name":"HAMAR",
                     "countyCode":"04"
                  },
                  "county":{
                     "code":"04",
                     "name":"HEDMARK"
                  }
               },
               {
                  "postalCode":"3671",
                  "city":"NOTODDEN",
                  "municipality":{
                     "code":"0807",
                     "name":"NOTODDEN",
                     "countyCode":"08"
                  },
                  "county":{
                     "code":"08",
                     "name":"TELEMARK"
                  }
               },
               {
                  "postalCode":"3672",
                  "city":"NOTODDEN",
                  "municipality":{
                     "code":"0807",
                     "name":"NOTODDEN",
                     "countyCode":"08"
                  },
                  "county":{
                     "code":"08",
                     "name":"TELEMARK"
                  }
               },
               {
                  "postalCode":"3673",
                  "city":"NOTODDEN",
                  "municipality":{
                     "code":"0807",
                     "name":"NOTODDEN",
                     "countyCode":"08"
                  },
                  "county":{
                     "code":"08",
                     "name":"TELEMARK"
                  }
               },
               {
                  "postalCode":"1011",
                  "city":"OSLO",
                  "municipality":{
                     "code":"0301",
                     "name":"OSLO",
                     "countyCode":"03"
                  },
                  "county":{
                     "code":"03",
                     "name":"OSLO"
                  }
               }
            ]
        """.trimIndent()

        private val municipalsResponse = """
            [
               {
                  "code":"1818",
                  "name":"HERØY (NORDLAND)",
                  "countyCode":"18"
               },
               {
                  "code":"1903",
                  "name":"HARSTAD",
                  "countyCode":"19"
               },
               {
                  "code":"0631",
                  "name":"FLESBERG",
                  "countyCode":"06"
               },
               {
                  "code":"5014",
                  "name":"FRØYA",
                  "countyCode":"50"
               },
               {
                  "code":"1928",
                  "name":"TORSKEN",
                  "countyCode":"19"
               },
               {
                  "code":"5061",
                  "name":"RINDAL",
                  "countyCode":"50"
               },
               {
                  "code":"1219",
                  "name":"BØMLO",
                  "countyCode":"12"
               },
               {
                  "code":"0430",
                  "name":"STOR-ELVDAL",
                  "countyCode":"04"
               },
               {
                  "code":"1528",
                  "name":"SYKKYLVEN",
                  "countyCode":"15"
               },
               {
                  "code":"0217",
                  "name":"OPPEGÅRD",
                  "countyCode":"02"
               },
               {
                  "code":"0515",
                  "name":"VÅGÅ",
                  "countyCode":"05"
               },
               {
                  "code":"5047",
                  "name":"OVERHALLA",
                  "countyCode":"50"
               },
               {
                  "code":"1840",
                  "name":"SALTDAL",
                  "countyCode":"18"
               },
               {
                  "code":"0815",
                  "name":"KRAGERØ",
                  "countyCode":"08"
               },
               {
                  "code":"1234",
                  "name":"GRANVIN",
                  "countyCode":"12"
               },
               {
                  "code":"1120",
                  "name":"KLEPP",
                  "countyCode":"11"
               },
               {
                  "code":"0532",
                  "name":"JEVNAKER",
                  "countyCode":"05"
               },
               {
                  "code":"1141",
                  "name":"FINNØY",
                  "countyCode":"11"
               },
               {
                  "code":"1867",
                  "name":"BØ (NORDLAND)",
                  "countyCode":"18"
               },
               {
                  "code":"0428",
                  "name":"TRYSIL",
                  "countyCode":"04"
               }
            ]
        """.trimIndent()

        private val countriesResponse = """
            [
               {
                  "code":"AD",
                  "name":"Andorra"
               }, 
               {
                  "code":"NO",
                  "name":"Norge"
               }
            ]
        """.trimIndent()

        private val countiesResponse = """
            [
               {
                  "code":"03",
                  "name":"OSLO"
               }, 
               {
                  "code":"42",
                  "name":"Agder"
               }
            ]
        """.trimIndent()
    }
}
