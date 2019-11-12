package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.config.MockConfig.Companion.sokResponse
import no.nav.rekrutteringsbistand.api.support.config.MockConfig.Companion.stillingResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock", "local")
internal class StillingControllerTest {

    @LocalServerPort
    var port = 0

    private fun localBaseUrl(): String = "http://localhost:$port/rekrutteringsbistand-api"

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("${localBaseUrl()}/local/cookie-isso", String::class.java)
    }

    @Test
    fun hentStillingReturnererStilling() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/ads?a=a", String::class.java).apply {
            assertThat(this).isEqualToIgnoringWhitespace(stillingResponse)
        }

    }

    @Test
    fun hentStillingReturnererSok() {
        val headers = HttpHeaders()

        val request = HttpEntity("body", headers)
        restTemplate.postForObject("${localBaseUrl()}/search-api/underenhet/_search", request, String::class.java).apply {
            assertThat(this).isEqualToIgnoringWhitespace(sokResponse)
        }

    }

}
