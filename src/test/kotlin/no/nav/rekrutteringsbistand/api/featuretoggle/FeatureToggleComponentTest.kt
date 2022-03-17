package no.nav.rekrutteringsbistand.api.featuretoggle

import no.nav.rekrutteringsbistand.api.config.MockLogin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeatureToggleComponentTest {

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    @Before
    fun authenticateClient() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun `isEnabled skal returnere HTTP 200 og om en feature er slått på`() {
        val feature = "rekrutteringsbistand.opprett-kandidatliste-knapp"
        val respons: ResponseEntity<Boolean> = restTemplate.getForEntity("$localBaseUrl/features/$feature", Boolean::class.java)
        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isTrue()
    }
}
