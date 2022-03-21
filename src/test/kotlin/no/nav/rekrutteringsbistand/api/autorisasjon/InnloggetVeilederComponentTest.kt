package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.Testdata.enVeileder
import no.nav.rekrutteringsbistand.api.config.MockLogin
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InnloggetVeilederComponentTest {

    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    private fun localBaseUrl(): String = "http://localhost:$port"

    @Before
    fun authenticateClient() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun hentInnloggetBrukerReturnererBruker() {
        restTemplate.getForObject("${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee", InnloggetVeileder::class.java).apply {
            assertThat(this).isEqualTo(enVeileder)
        }
    }
}
