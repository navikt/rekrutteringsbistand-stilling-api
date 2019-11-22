package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption.ENABLE_COOKIES
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local", "kandidatlisteMock")
class StillingsinfoComponentTest {

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    val restTemplate = TestRestTemplate(ENABLE_COOKIES)

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/local/cookie-isso", String::class.java)
    }

    @Test
    fun `Henting av rekrutteringsbistand basert på stilling skal returnere HTTP status 200 og JSON med nyopprettet rekrutteringUuid`() {
        // Given
        val lagre = Testdata.enStillingsinfo
        repository.lagre(lagre)

        val url = "$localBaseUrl/rekruttering/stilling/${lagre.stillingsid}"

        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()

        val httpEntity = HttpEntity(null, headers)

        // When
        val actualResponse = restTemplate.exchange(url, HttpMethod.GET, httpEntity, StillingsinfoDto::class.java)

        // Then
        assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(actualResponse.hasBody())
        actualResponse.body!!.apply {
            assertThat(stillingsinfoid).isNotBlank()
            assertDoesNotThrow { UUID.fromString(stillingsinfoid) }
            assertThat(this).isEqualTo(lagre.asDto())
            repository.slett(lagre.stillingsinfoid)
        }
    }

    @Test
    fun `Henting av rekrutteringsbistand basert på bruker skal returnere HTTP status 200 og JSON med nyopprettet rekrutteringUuid`() {
        // Given
        val lagre = Testdata.enStillingsinfo
        repository.lagre(lagre)

        val url = "$localBaseUrl/rekruttering/ident/${lagre.eier.navident}"

        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()

        val httpEntity = HttpEntity(null, headers)

        // When
        val actualResponse =
                restTemplate.run { exchange(url, HttpMethod.GET, httpEntity, object : ParameterizedTypeReference<List<StillingsinfoDto>>() {}) }

        // Then
        assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(actualResponse.hasBody())
        actualResponse.body!!.apply {
            assertThat(this).hasSize(1)
            this.get(0).apply {
                assertThat(stillingsinfoid).isNotBlank()
                assertDoesNotThrow { UUID.fromString(stillingsinfoid) }
                assertThat(this).isEqualTo(lagre.asDto())
                repository.slett(lagre.stillingsinfoid)
            }
        }
    }


    @Test
    fun `Lagring av rekrutteringsbistand skal returnere HTTP status 201 og JSON med nyopprettet rekrutteringUuid`() {
        // Given
        val tilLagring = Testdata.enStillingsinfo.asDto().copy(stillingsinfoid = null)
        val url = "$localBaseUrl/rekruttering"

        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()
        val httpEntity = HttpEntity(tilLagring, headers)

        // When
        val actualResponse = restTemplate.postForEntity(url, httpEntity, StillingsinfoDto::class.java)

        // Then
        assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(actualResponse.hasBody())
        actualResponse.body!!.apply {
            assertThat(stillingsinfoid).isNotBlank()
            assertDoesNotThrow { UUID.fromString(stillingsinfoid) }
            assertThat(this.copy(stillingsinfoid = null))
                    .isEqualTo(tilLagring)
        }

        val oppdatert = repository.hentForStilling(Testdata.enStillingsinfo.stillingsid).getOrElse { fail("fant ikke stillingen") }
        oppdatert.apply {
            assertThat(stillingsinfoid.asString()).isNotBlank()
            assertThat(this.asDto())
                    .isEqualTo(actualResponse.body!!)
            repository.slett(this.stillingsinfoid)
        }
    }

    @Test
    fun `Oppdatering av rekrutteringsbistand skal returnere HTTP status 200 og JSON med oppdatert rekrutteringUuid`() {

        // Given
        val lagre = Testdata.enStillingsinfo
        val oppdatere = lagre.copy(eier = Eier(navident = "endretIdent", navn = "endretNavn"))
        val lagret = repository.lagre(lagre)

        val url = "$localBaseUrl/rekruttering"

        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()
        val httpEntity = HttpEntity(oppdatere.asDto(), headers)

        // When
        val actualResponse = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, StillingsinfoDto::class.java)

        // Then
        assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(actualResponse.hasBody())
        actualResponse.body!!.apply {
            assertThat(stillingsinfoid).isNotBlank()
            assertDoesNotThrow { UUID.fromString(stillingsinfoid) }
            assertThat(this).isEqualTo(oppdatere.asDto())
        }

        val oppdatert = repository.hentForStilling(oppdatere.stillingsid).getOrElse { fail("fant ikke stillingen") }
        oppdatert.apply {
            assertThat(stillingsinfoid.asString()).isNotBlank()
            assertThat(this)
                    .isEqualTo(oppdatere)
            repository.slett(this.stillingsinfoid)
        }
    }

}
