package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption.ENABLE_COOKIES
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingsinfoComponentTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(8766))

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    val restTemplate = TestRestTemplate(ENABLE_COOKIES)

    @MockBean
    lateinit var arbeidsplassenKlient: ArbeidsplassenKlient

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/veileder-token-cookie", Unit::class.java)
    }

    @Test
    fun `Henting av eier basert på stilling skal returnere HTTP OK med lagret stillingsinfo`() {
        repository.opprett(enStillingsinfo)

        val url = "$localBaseUrl/rekruttering/stilling/${enStillingsinfo.stillingsid}"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.GET, httpEntity(null), EierDto::class.java)

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(stillingsinfoRespons.body).isEqualTo(enStillingsinfo.asEierDto())
    }

    @Test
    fun `Henting av stillingsinfo basert på bruker skal returnere HTTP 200 med lagret stillingsinfo`() {
        repository.opprett(enStillingsinfo)

        val url = "$localBaseUrl/rekruttering/ident/${enStillingsinfo.eier?.navident}"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.GET, httpEntity(null), object : ParameterizedTypeReference<List<EierDto>>() {})

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        stillingsinfoRespons.body.apply {
            assertThat(this!!).contains(enStillingsinfo.asEierDto())
        }
    }

    @Test
    fun `Opprettelse av kandidatliste på ekstern stilling skal returnere HTTP 201 med opprettet stillingsinfo, og trigge resending hos Arbeidsplassen`() {
        val dto = enStillingsinfoInboundDto

        mockKandidatlisteOppdatering()

        val url = "$localBaseUrl/rekruttering"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.PUT, httpEntity(dto), StillingsinfoDto::class.java)

        verify(arbeidsplassenKlient, times(1)).triggResendingAvStillingsmeldingFraArbeidsplassen(dto.stillingsid)
        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.CREATED)

        stillingsinfoRespons.body!!.apply {
            assertThat(this.stillingsid).isNotNull
            assertThat(this.eierNavn).isEqualTo(dto.eierNavn)
            assertThat(this.eierNavident).isEqualTo(dto.eierNavident)
            assertThat(this.notat).isNull()
        }
    }

    @Test
    fun `Oppretting av kandidatliste på ekstern stilling med eksisterende stillingsinfo skal returnere HTTP 201 med oppdatert stillingsinfo`() {
        repository.opprett(enStillingsinfo)
        val tilLagring = enStillingsinfo.asEierDto().copy(stillingsinfoid = null)
        mockKandidatlisteOppdatering()

        val url = "$localBaseUrl/rekruttering"
        val stillingsinfoRespons = restTemplate.exchange(url, HttpMethod.PUT, httpEntity(tilLagring), StillingsinfoDto::class.java)
        val lagretStillingsinfo = repository.hentForStilling(enStillingsinfo.stillingsid).getOrElse { fail("fant ikke stillingen") }

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(stillingsinfoRespons.body).isEqualTo(lagretStillingsinfo.asStillingsinfoDto())
    }

    @After
    fun tearDown() {
        testRepository.slettAlt()
    }

    private fun httpEntity(body: Any?): HttpEntity<Any> {
        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON_VALUE,
                ACCEPT to APPLICATION_JSON_VALUE
        ).toMultiValueMap()
        return HttpEntity(body, headers)
    }

    private fun mockKandidatlisteOppdatering() {
        wiremock.stubFor(
                put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste"))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse().withStatus(NO_CONTENT.value())
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        )
    }
}
