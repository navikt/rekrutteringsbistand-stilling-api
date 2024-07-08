package no.nav.rekrutteringsbistand.api.autorisasjon

import io.ktor.http.*
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TilgangTest {


    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    private fun localBaseUrl(): String = "http://localhost:$port"


    fun gjørKall(token: String?, responseEntityFunksjon: ResponseEntity<*>.() -> Unit) =
        restTemplate.exchange(
            "${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee",
            HttpMethod.GET,
            HttpEntity(null, token?.let { mapOf("Authorization" to "Bearer $it").toMultiValueMap() }),
            String::class.java
        ).run(responseEntityFunksjon)

    @Test
    fun `Kall med token skal få 200 OK`() = gjørKall(mockLogin.hentAzureAdVeilederToken()) {
        Assertions.assertThat(statusCode.value()).isEqualTo(200)
    }

    @Test
    fun `Kall uten token skal få 401 Unauthorized`() = gjørKall(null) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med utdatert token skal få 401 Unauthorized`() = gjørKall(mockLogin.hentAzureAdVeilederToken(expiry = -60)) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med feil audience skal få 401 Unauthorized`() = gjørKall(mockLogin.hentAzureAdVeilederToken(audience = "feil audience")) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med feil algoritme skal få 401 Unauthorized`() {
        val tokenMedFeilAlgoritme = mockLogin.hentAzureAdVeilederToken().split(".").let { (_, payload) ->
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.$payload."
        }
        gjørKall(tokenMedFeilAlgoritme) {
            Assertions.assertThat(statusCode.value()).isEqualTo(401)
        }
    }

    @Test
    fun `Kall med feil issuer skal få 401 Unauthorized`() {
        val feilOauthserver = MockOAuth2Server()
        try {
            feilOauthserver.start(port = 12345)
            val token = feilOauthserver.issueToken(
                issuerId = azureAdIssuer,
                subject = "brukes-ikke",
                claims = mapOf(
                    "unique_name" to "Clark.Kent@nav.no",
                    "NAVident" to "C12345",
                    "name" to "Clark Kent"
                ),
                audience = "default"
            ).serialize()
            gjørKall(token) {
                Assertions.assertThat(statusCode.value()).isEqualTo(401)
            }
        } finally {
            feilOauthserver.shutdown()
        }
    }
}