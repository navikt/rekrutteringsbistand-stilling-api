package no.nav.rekrutteringsbistand.api

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType

fun mockAzureObo(wiremockAzure: WireMockRule) {
    wiremockAzure.stubFor(
        WireMock.request(HttpMethod.POST.name(), WireMock.urlPathMatching("/token"))
            .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .willReturn(
                WireMock.aResponse().withStatus(200)
                    .withHeader(HttpHeaders.CONNECTION, "close")
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        """
                            {
                                "access_token": "eksempeltoken"
                            }
                        """.trimIndent()
                    )
            )
    )
}

fun mockAzureObo(wireMockExtension: WireMockExtension) {
    wireMockExtension.stubFor(
        WireMock.request(HttpMethod.POST.name(), WireMock.urlPathMatching("/token"))
            .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.containing(MediaType.APPLICATION_FORM_URLENCODED_VALUE))
            .willReturn(
                WireMock.aResponse().withStatus(200)
                    .withHeader(HttpHeaders.CONNECTION, "close")
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        """
                            {
                                "access_token": "eksempeltoken"
                            }
                        """.trimIndent()
                    )
            )
    )
}
