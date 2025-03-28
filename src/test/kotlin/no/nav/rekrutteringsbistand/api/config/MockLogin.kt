package no.nav.rekrutteringsbistand.api.config

import no.nav.rekrutteringsbistand.api.autorisasjon.azureAdIssuer
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.MockOAuth2ServerAutoConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.stereotype.Component

const val jobbsøkerrettet = "0dba8374-bf36-4d89-bbba-662447d57b94"
const val arbeidsgiverrettet = "52bc2af7-38d1-468b-b68d-0f3a4de45af2"
const val utvikler = "a1749d9a-52e0-4116-bb9f-935c38f6c74a"

@Import(MockOAuth2ServerAutoConfiguration::class)
@Component
class MockLogin(private val mockOauth2Server: MockOAuth2Server) {

    fun hentAzureAdVeilederToken(expiry: Long = 3600, audience: String = "default", roller: List<String> = listOf(arbeidsgiverrettet)): String {
        return mockOauth2Server.issueToken(
            issuerId = azureAdIssuer,
            subject = "brukes-ikke",
            claims = mapOf(
                "unique_name" to "Clark.Kent@nav.no",
                "NAVident" to "C12345",
                "name" to "Clark Kent",
                "groups" to roller
            ),
            expiry = expiry,
            audience = audience
        ).serialize()
    }

    fun leggAzureVeilederTokenPåAlleRequests(testRestTemplate: TestRestTemplate, roller: List<String> = listOf(arbeidsgiverrettet)) {
        val token = hentAzureAdVeilederToken(roller = roller)

        testRestTemplate.restTemplate.interceptors.add(ClientHttpRequestInterceptor { request, body, execution ->
            request.headers.set("Authorization", "Bearer $token")
            execution.execute(request, body)
        })
    }

    fun hentAzureAdMaskinTilMaskinToken(clientUri: String): String {
        return mockOauth2Server.issueToken(
            azureAdIssuer,
            "dummyClientId",
            DefaultOAuth2TokenCallback(
                claims = mapOf("azp_name" to clientUri)
            )
        ).serialize()
    }
}
