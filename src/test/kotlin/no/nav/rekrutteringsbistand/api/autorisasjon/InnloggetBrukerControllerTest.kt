package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("stillingMock", "local")
internal class InnloggetBrukerControllerTest {

    @LocalServerPort
    var port = 0

    private fun localBaseUrl(): String = "http://localhost:$port/rekrutteringsbistand-api"

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("${localBaseUrl()}/local/cookie-isso", String::class.java)
    }

    @Test
    fun hentInnloggetBrukerReturnererBruker() {
        restTemplate.getForObject( "${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee", InnloggetBruker::class.java).apply {
            assertThat(this).isEqualTo(Testdata.enVeileder)
        }
    }

    @Test
    fun tokenLever() {
        restTemplate.getForObject( "${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee/token-expiring", Boolean::class.java).apply {
            assertThat(this).isFalse()
        }
    }

}
