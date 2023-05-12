package no.nav.rekrutteringsbistand.api.standardsøk

import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandardsøkTest {

    @Autowired
    lateinit var standardsøkRepository: StandardsøkRepository

    @Autowired
    lateinit var mockLogin: MockLogin

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate()

    @Before
    fun authenticateClient() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun `PUT til standardsøk skal lagre nytt standardsøk for navIdent hvis ingen søk er lagret fra før på denne personen`() {
        val standardsøkTilLagring = LagreStandardsøkDto("?fritekst=jalla&publisert=intern")

        val response: ResponseEntity<HentStandardsøkDto> = restTemplate.exchange(
                "$localBaseUrl/standardsok",
                HttpMethod.PUT,
                HttpEntity(standardsøkTilLagring),
                HentStandardsøkDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.søk).isEqualTo(standardsøkTilLagring.søk)
        assertThat(response.body?.navIdent).isEqualTo("C12345")
        assertThat(response.body?.tidspunkt).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now())
    }

    @Test
    fun `PUT til standardsøk skal endre eksisterende standardsøk for navIdent hvis søk er lagret fra før på denne personen`() {
        val standardsøkTilLagring = LagreStandardsøkDto("?fritekst=jalla&publisert=intern")
        standardsøkRepository.oppdaterStandardsøk(standardsøkTilLagring, "C12345")

        val nyttStandardsøkTilLagring = LagreStandardsøkDto("?publisert=intern")
        val response: ResponseEntity<HentStandardsøkDto> = restTemplate.exchange(
                "$localBaseUrl/standardsok",
                HttpMethod.PUT,
                HttpEntity(nyttStandardsøkTilLagring),
                HentStandardsøkDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(response.body?.søk).isEqualTo(nyttStandardsøkTilLagring.søk)
        assertThat(response.body?.navIdent).isEqualTo("C12345")
        assertThat(response.body?.tidspunkt).isBetween(LocalDateTime.now().minusSeconds(5), LocalDateTime.now())
    }

    @Test
    fun `GET til standardsøk skal hente lagret standardsøk for navIdent`() {
        val standardsøkTilLagring = LagreStandardsøkDto("?fritekst=jalla&publisert=intern")
        standardsøkRepository.oppdaterStandardsøk(standardsøkTilLagring, "C12345")

        val response: ResponseEntity<HentStandardsøkDto> = restTemplate.getForEntity(
                "$localBaseUrl/standardsok",
                HentStandardsøkDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.søk).isEqualTo(standardsøkTilLagring.søk)
        assertThat(response.body?.navIdent).isEqualTo("C12345")
    }

    @Test
    fun `GET til standardsøk med azuread-token skal hente lagret standardsøk for navIdent`() {
        val restTemplateUtenCookie = TestRestTemplate()
        val token = mockLogin.hentAzureAdVeilederToken("C12345")

        val standardsøkTilLagring = LagreStandardsøkDto("?fritekst=jalla&publisert=intern")
        standardsøkRepository.oppdaterStandardsøk(standardsøkTilLagring, "C12345")

        val response = restTemplateUtenCookie.exchange(
            "$localBaseUrl/standardsok",
            HttpMethod.GET,
            HttpEntity(null, mapOf(
                AUTHORIZATION to "Bearer $token"
            ).toMultiValueMap()),
            HentStandardsøkDto::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.søk).isEqualTo(standardsøkTilLagring.søk)
        assertThat(response.body?.navIdent).isEqualTo("C12345")
    }
}