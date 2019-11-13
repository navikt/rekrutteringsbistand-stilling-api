package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.Testdata.etRekrutteringsbistand
import no.nav.rekrutteringsbistand.api.Testdata.rekrutteringsDto
import no.nav.rekrutteringsbistand.api.rekrutteringsbistand.RekrutteringsbistandRepository
import no.nav.rekrutteringsbistand.api.support.config.MockConfig.Companion.sokResponse
import no.nav.rekrutteringsbistand.api.support.config.MockConfig.Companion.stillingerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock", "local")
internal class StillingControllerTest {

    @LocalServerPort
    var port = 0

    private fun localBaseUrl(): String = "http://localhost:$port/rekrutteringsbistand-api"

    @Autowired
    lateinit var repository: RekrutteringsbistandRepository

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("${localBaseUrl()}/local/cookie-isso", String::class.java)
    }

    @Test
    fun hentStilling_skal_returnere_stilling() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/ads?a=a", String::class.java).apply {
            assertThat(this).isEqualToIgnoringWhitespace(stillingerResponse)
        }
    }

    @Test
    fun hentStilling_skal_returnere_stilling_med_rekrutteringsbistand() {
        repository.lagre(etRekrutteringsbistand)

        val stilling = restTemplate.exchange(
                "${localBaseUrl()}/rekrutteringsbistand/api/v1/ads?a=a",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<Stilling>>() {}
        ).body

        assertThat(stilling!!.content.first().rekruttering).isEqualTo(rekrutteringsDto(etRekrutteringsbistand))
        assertThat(stilling.content.first().uuid).isEqualTo(etRekrutteringsbistand.stillingUuid)

        repository.slett(etRekrutteringsbistand.rekrutteringUuid!!)
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
