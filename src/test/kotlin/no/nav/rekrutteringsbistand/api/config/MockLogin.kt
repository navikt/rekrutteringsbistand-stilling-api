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
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

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

    fun leggAzureVeilederTokenPåAlleRequests(restTemplate: RestTemplate) {
        val token = hentAzureAdVeilederToken()

        restTemplate.interceptors.add(object: ClientHttpRequestInterceptor {
            override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
                request.headers.set("Authorization", "Bearer $token")
                return execution.execute(request, body)
            }
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
