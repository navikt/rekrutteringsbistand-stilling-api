package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
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
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local")
class StillingsinfoComponentTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(9924))

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
    fun `Henting av stillingsinfo basert på stilling skal returnere HTTP OK med lagret stillingsinfo`() {
        repository.lagre(enStillingsinfo)

        val url = "$localBaseUrl/rekruttering/stilling/${enStillingsinfo.stillingsid}"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.GET, httpEntity(null), StillingsinfoDto::class.java)

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(stillingsinfoRespons.body).isEqualTo(enStillingsinfo.asDto())
    }

    @Test
    fun `Henting av stillingsinfo basert på bruker skal returnere HTTP 200 med lagret stillingsinfo`() {
        repository.lagre(enStillingsinfo)

        val url = "$localBaseUrl/rekruttering/ident/${enStillingsinfo.eier.navident}"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.GET, httpEntity(null), object : ParameterizedTypeReference<List<StillingsinfoDto>>() {})

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        stillingsinfoRespons.body.apply {
            assertThat(this).hasSize(1)
            assertThat(this!![0]).isEqualTo(enStillingsinfo.asDto())
        }
    }


    @Test
    fun `Opprettelse av stillingsinfo skal returnere HTTP 201 med opprettet stillingsinfo`() {
        val tilLagring = enStillingsinfo.asDto().copy(stillingsinfoid = null)
        mockKandidatlisteOppdatering()

        val url = "$localBaseUrl/rekruttering"
        val stillingsinfoRespons = restTemplate.postForEntity(url, httpEntity(tilLagring), StillingsinfoDto::class.java)
        val lagretStillingsinfo = repository.hentForStilling(enStillingsinfo.stillingsid).getOrElse { fail("fant ikke stillingen") }

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.CREATED)
        stillingsinfoRespons.body!!.apply {
            assertThat(this).isEqualToIgnoringGivenFields(tilLagring, "stillingsinfoid")
            assertThat(stillingsinfoid).isEqualTo(lagretStillingsinfo.stillingsinfoid.asString())
        }

        repository.slett(lagretStillingsinfo.stillingsinfoid)
    }

    @Test
    fun `oppdatering av rekrutteringsbistand skal returnere HTTP status 200 og JSON med oppdatert rekrutteringUuid`() {

        // Given
        val lagre = enStillingsinfo
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

    @After
    fun tearDown() {
        repository.slett(enStillingsinfo.stillingsinfoid)
    }

    private fun httpEntity(body: Any?): HttpEntity<Any> {
        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()
        return HttpEntity(body, headers)
    }

    private fun mockKandidatlisteOppdatering() {
        wiremock.stubFor(
                put(urlPathMatching("/pam-kandidatsok-api/rest/veileder/stilling/.*/kandidatliste"))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON.toString()))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON.toString()))
                        .willReturn(aResponse().withStatus(NO_CONTENT.value())
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        )
    }
}
