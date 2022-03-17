package no.nav.rekrutteringsbistand.api.config

import no.nav.rekrutteringsbistand.api.autorisasjon.azureAdIssuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.bind.annotation.RestController

@Import(MockOAuth2ServerAutoConfiguration::class)
@RestController
class MockLogin(val mockOauth2Server: MockOAuth2Server) {

    fun hentAzureAdVeilederToken(): String {
        return mockOauth2Server.issueToken(
            issuerId = azureAdIssuer,
            subject = "brukes-ikke",
            claims = mapOf(
                "unique_name" to "Clark.Kent@nav.no",
                "NAVident" to "C12345",
                "name" to "Clark Kent"
            )
        ).serialize()
    }

    fun leggAzureVeilederTokenPåAlleRequests(testRestTemplate: TestRestTemplate) {
        val token = hentAzureAdVeilederToken()

        testRestTemplate.restTemplate.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.set("Authorization", "Bearer $token")
            execution.execute(request, body)
        })
    }

    fun hentAzureAdMaskinTilMaskinToken(clientId: String): String {
        return mockOauth2Server.issueToken(
                azureAdIssuer,
                clientId,
                DefaultOAuth2TokenCallback()
        ).serialize()
    }
}
