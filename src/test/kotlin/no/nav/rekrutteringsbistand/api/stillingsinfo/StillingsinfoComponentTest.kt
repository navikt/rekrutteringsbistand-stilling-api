package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingsinfoComponentTest {

    @get:Rule
    val wiremockKandidatApi = WireMockRule(WireMockConfiguration.options().port(8766))

    @get:Rule
    val wiremockPamAdApi = WireMockRule(WireMockConfiguration.options().port(8765))

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    @MockBean
    lateinit var arbeidsplassenKlient: ArbeidsplassenKlient

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @MockBean
    private lateinit var kandidatlisteKlient: KandidatlisteKlient

    private val objectMapper: ObjectMapper =
        ObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Before
    fun authenticateClient() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun `Henting av stillingsinfo basert på bruker skal returnere HTTP 200 med lagret stillingsinfo`() {
        repository.opprett(enStillingsinfo)

        val url = "$localBaseUrl/stillingsinfo/ident/${enStillingsinfo.eier?.navident}"
        val stillingsinfoRespons = restTemplate.exchange(
            url,
            HttpMethod.GET,
            httpEntity(null),
            object : ParameterizedTypeReference<List<StillingsinfoDto>>() {})

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        stillingsinfoRespons.body.apply {
            assertThat(this!!).contains(enStillingsinfo.asStillingsinfoDto())
        }
    }

    @Test
    fun `Opprettelse av kandidatliste på ekstern stilling skal returnere HTTP 201 med opprettet stillingsinfo, og trigge resending hos Arbeidsplassen`() {
        val dto = enStillingsinfoInboundDto

        mockAzureObo(wiremockAzure)

        val url = "$localBaseUrl/stillingsinfo"
        val stillingsinfoRespons =
            restTemplate.exchange(url, HttpMethod.PUT, httpEntity(dto), StillingsinfoDto::class.java)

        verify(arbeidsplassenKlient, times(1)).triggResendingAvStillingsmeldingFraArbeidsplassen(dto.stillingsid)
        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)

        stillingsinfoRespons.body!!.apply {
            assertThat(this.stillingsid).isNotNull
            assertThat(this.eierNavn).isEqualTo(dto.eierNavn)
            assertThat(this.eierNavident).isEqualTo(dto.eierNavident)
            assertThat(this.notat).isNull()
        }
    }

    @Test
    fun `Oppretting av kandidatliste på ekstern stilling med eksisterende stillingsinfo skal returnere HTTP 200 med oppdatert stillingsinfo`() {
        repository.opprett(enStillingsinfo)
        val tilLagring = enStillingsinfoInboundDto
        val stilling = enStilling
        mockAzureObo(wiremockAzure)
        mockPamAdApi(HttpMethod.GET, "/api/v1/ads/${stilling.uuid}", stilling)

        val url = "$localBaseUrl/stillingsinfo"
        val stillingsinfoRespons =
            restTemplate.exchange(url, HttpMethod.PUT, httpEntity(tilLagring), StillingsinfoDto::class.java)
        val lagretStillingsinfo =
            repository.hentForStilling(enStillingsinfo.stillingsid).getOrElse { fail("fant ikke stillingen") }

        assertThat(stillingsinfoRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(stillingsinfoRespons.body).isEqualTo(lagretStillingsinfo.asStillingsinfoDto())
    }

    @Test
    fun `Når vi prøver å opprette eier og kall mot kandidat-api feiler så skal ingenting ha blitt lagret`() {
        val dto = enStillingsinfoInboundDto
        mockAzureObo(wiremockAzure)
        `when`(kandidatlisteKlient.sendStillingOppdatert(enRekrutteringsbistandStilling)).thenThrow(RuntimeException::class.java)

        val respons =
            restTemplate.exchange("$localBaseUrl/stillingsinfo", HttpMethod.PUT, httpEntity(dto), String::class.java)

        assertThat(respons.statusCodeValue).isEqualTo(500)
        val stillingsinfo = repository.hentForStilling(Stillingsid(dto.stillingsid)).orNull()
        assertThat(stillingsinfo).isNull()
    }

    @Test
    fun `Når vi prøver å endre eier og kall mot kandidat-api feiler så skal ingen endringer ha blitt lagret`() {
        val stillingsinfo = enStillingsinfo.copy(eier = Eier("Y111111", "Et Navn"))
        repository.opprett(stillingsinfo)
        val endringDto = StillingsinfoInboundDto(
            stillingsid = stillingsinfo.stillingsid.asString(),
            eierNavident = "X998877",
            eierNavn = "Helt Annet Navn"
        )
        mockAzureObo(wiremockAzure)
        `when`(kandidatlisteKlient.sendStillingOppdatert(enRekrutteringsbistandStilling)).thenThrow(RuntimeException::class.java)

        val respons = restTemplate.exchange("$localBaseUrl/stillingsinfo", HttpMethod.PUT, httpEntity(endringDto), String::class.java)

        assertThat(respons.statusCodeValue).isEqualTo(500)
        val lagretStillingsinfo = repository.hentForStilling(stillingsinfo.stillingsid).orNull()
        assertThat(lagretStillingsinfo!!.eier).isEqualTo(stillingsinfo.eier)
    }

    @Test
    fun `Når vi prøver å endre eier og kall mot kandidat-api feiler så skal ingen endringer ha blitt lagret gitt at eksisterende eier er null`() {
        val stillingsinfoDerEierErNull = enStillingsinfo.copy(eier = null)
        repository.opprett(stillingsinfoDerEierErNull)
        mockAzureObo(wiremockAzure)
        val endringDto = StillingsinfoInboundDto(
            stillingsid = stillingsinfoDerEierErNull.stillingsid.asString(),
            eierNavident = "X998877",
            eierNavn = "Helt Annet Navn"
        )
        val rekrutteringsbistandStilling = enRekrutteringsbistandStilling.copy(stillingsinfo = stillingsinfoDerEierErNull.asStillingsinfoDto())
        `when`(kandidatlisteKlient.sendStillingOppdatert(rekrutteringsbistandStilling)).thenThrow(RuntimeException::class.java)

        val respons = restTemplate.exchange("$localBaseUrl/stillingsinfo", HttpMethod.PUT, httpEntity(endringDto), String::class.java)

        assertThat(respons.statusCodeValue).isEqualTo(500)
        val lagretStillingsinfo = repository.hentForStilling(stillingsinfoDerEierErNull.stillingsid).orNull()
        assertThat(lagretStillingsinfo!!.eier).isNull()
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

    private fun mockPamAdApi(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            WireMock.request(method.name, WireMock.urlPathMatching(urlPath))
                .withHeader(CONTENT_TYPE, WireMock.equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, WireMock.equalTo(APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION,
                    WireMock.matching("Bearer .*")
                )
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(
                        CONNECTION, "close"
                    )
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )
    }
}
