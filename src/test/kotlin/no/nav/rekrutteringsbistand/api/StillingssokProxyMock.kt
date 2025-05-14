package no.nav.rekrutteringsbistand.api

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

class StillingssokProxyMock {

    fun mockStillingssokProxy(wireMockExtension: WireMockExtension, urlPath: String) {
        wireMockExtension.stubFor(
            request(HttpMethod.GET.name(), urlPathMatching(urlPath)).withHeader(
                CONTENT_TYPE, equalTo(
                    APPLICATION_JSON_VALUE
                )
            )
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(
                    aResponse().withStatus(200).withHeader(
                        CONNECTION, "close"
                    ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(Testdata.esResponse)
                )
        )
    }
}
