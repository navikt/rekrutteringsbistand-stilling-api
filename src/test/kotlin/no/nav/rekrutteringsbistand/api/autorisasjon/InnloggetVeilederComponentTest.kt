package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.Testdata.enVeileder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InnloggetVeilederComponentTest {

    @LocalServerPort
    var port = 0

    private fun localBaseUrl(): String = "http://localhost:$port"

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForEntity("${localBaseUrl()}/veileder-token-cookie", Unit::class.java)
    }

    @Test
    fun hentInnloggetBrukerReturnererBruker() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee", InnloggetVeileder::class.java).apply {
            assertThat(this).isEqualTo(enVeileder)
        }
    }
}
