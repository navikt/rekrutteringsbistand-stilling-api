package no.nav.rekrutteringsbistand.api.autorisasjon

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.*
import io.ktor.utils.io.core.*
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.autorisasjon.StatusType.*
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.config.arbeidsgiverrettet
import no.nav.rekrutteringsbistand.api.config.jobbsøkerrettet
import no.nav.rekrutteringsbistand.api.config.utvikler
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.standardsøk.LagreStandardsøkDto
import no.nav.rekrutteringsbistand.api.standardsøk.StandardsøkRepository
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.rekrutteringsbistand.api.stillingsinfo.indekser.BulkStillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

private val objectMapper: ObjectMapper =
    ObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

private const val navIdent = "C12345"

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TilgangTest {


    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var mockLogin: MockLogin

    @Autowired
    private lateinit var webClient: WebTestClient

    @MockBean
    private lateinit var kandidatlisteKlient: KandidatlisteKlient

    @MockBean
    private lateinit var azureKlient: AzureKlient

    @Autowired
    private lateinit var repository: TestRepository

    @Autowired
    private lateinit var standardsøkRepository: StandardsøkRepository

    private lateinit var stubber: Stubber

    private val restTemplate = TestRestTemplate()

    private fun localBaseUrl(): String = "http://localhost:$port"

    @BeforeAll
    fun setup() {
        stubber = Stubber(kandidatlisteKlient, azureKlient, standardsøkRepository)
    }

    @BeforeEach
    fun preTest() {
        stubber.resetAll()
        Mockito.reset(kandidatlisteKlient, azureKlient)
        repository.slettAlt()
    }

    @AfterAll
    fun afterAll() {
        stubber.close()
    }

    fun gjørKall(token: String?, responseEntityFunksjon: ResponseEntity<*>.() -> Unit) =
        restTemplate.exchange(
            "${localBaseUrl()}/rekrutteringsbistand/api/v1/reportee",
            HttpMethod.GET,
            HttpEntity(null, token?.let { mapOf("Authorization" to "Bearer $it").toMultiValueMap() }),
            String::class.java
        ).run(responseEntityFunksjon)

    @Test
    fun `Kall med token skal få 200 OK`() = gjørKall(mockLogin.hentAzureAdVeilederToken()) {
        Assertions.assertThat(statusCode.value()).isEqualTo(200)
    }

    @Test
    fun `Kall uten token skal få 401 Unauthorized`() = gjørKall(null) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med utdatert token skal få 401 Unauthorized`() = gjørKall(mockLogin.hentAzureAdVeilederToken(expiry = -60)) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med feil audience skal få 401 Unauthorized`() = gjørKall(mockLogin.hentAzureAdVeilederToken(audience = "feil audience")) {
        Assertions.assertThat(statusCode.value()).isEqualTo(401)
    }

    @Test
    fun `Kall med feil algoritme skal få 401 Unauthorized`() {
        val tokenMedFeilAlgoritme = mockLogin.hentAzureAdVeilederToken().split(".").let { (_, payload) ->
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJub25lIn0.$payload."
        }
        gjørKall(tokenMedFeilAlgoritme) {
            Assertions.assertThat(statusCode.value()).isEqualTo(401)
        }
    }

    @Test
    fun `Kall med feil issuer skal få 401 Unauthorized`() {
        val feilOauthserver = MockOAuth2Server()
        try {
            feilOauthserver.start(port = 12345)
            val token = feilOauthserver.issueToken(
                issuerId = azureAdIssuer,
                subject = "brukes-ikke",
                claims = mapOf(
                    "unique_name" to "Clark.Kent@nav.no",
                    "NAVident" to navIdent,
                    "name" to "Clark Kent"
                ),
                audience = "default"
            ).serialize()
            gjørKall(token) {
                Assertions.assertThat(statusCode.value()).isEqualTo(401)
            }
        } finally {
            feilOauthserver.shutdown()
        }
    }

    fun tilgangsTester() = Kall(webClient, mockLogin, stubber).run {
        listOf(
            stilling::opprettStilling to Varianter(forbidden, ok, ok, forbidden),
            stilling::opprettJobbmesse to Varianter(forbidden, ok, ok, forbidden),
            stilling::opprettFormidling to Varianter(ok, ok, ok, forbidden),
            stilling::oppdaterStilling to Varianter(forbidden, ok, ok, forbidden),
            stilling::kopierStilling to Varianter(forbidden, ok, ok, forbidden),
            stilling::slettStilling to Varianter(forbidden, ok, ok, forbidden),
            stilling::hentStillingMedUuid to Varianter(ok, ok, ok, ok),
            stilling::hentStillingMedAnnonsenr to Varianter(ok, ok, ok, ok),
            stilling::hentStillingForPersonBruker to Varianter(unauthorized, unauthorized, unauthorized, unauthorized),   //Hvis alt er forbidden kan testen liksågodt endres til dette
            stillingsInfo::overtaEierskapForEksternStillingOgKandidatliste to Varianter(forbidden, ok, ok, forbidden),
            indekser::hentStillingsinfoBulk to Varianter(unauthorized, unauthorized, unauthorized, unauthorized),   //Hvis alt er forbidden kan testen liksågodt endres til dette
            innloggetVeileder::hentInnloggetVeileder to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::kategorier to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::geografiPostnr to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::geografiKommuner to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::geografiFylker to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::geografiLand to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::getSøk to Varianter(ok, ok, ok, ok),
            arbeidsplassenProxy::postSøk to Varianter(ok, ok, ok, ok),
            standardSøk::hentStandardsøk to Varianter(ok, ok, ok, ok),
            standardSøk::upsertStandardsøk to Varianter(created, created, created, created)
        ).flatMap { (kall, svar) ->
            listOf(
                Arguments.of(kall.name, TestRolle.Jobbsøkerrettet, svar.jobbsøkerrettet, kall()),
                Arguments.of(kall.name, TestRolle.Arbeidsgiverrettet, svar.arbeidsgiverrettet, kall()),
                Arguments.of(kall.name, TestRolle.Utvikler, svar.utvikler, kall()),
                Arguments.of(kall.name, TestRolle.Ingen, svar.ingen, kall()),
            )
        }
    }

    @ParameterizedTest(name = "{index}: {0} med rolle {1} skal returnere {2}")
    @MethodSource("tilgangsTester")
    fun tilgangsTest(
        kallnavnBruktTilTestBeskrivelse: String,
        rolle: TestRolle,
        statusType: StatusType,
        kallFunksjon: EndepunktHandler,
    ) {
        statusType.assertion(kallFunksjon(rolle))
    }
}

enum class TestRolle(private val uuid: String) {
    Jobbsøkerrettet(jobbsøkerrettet),
    Arbeidsgiverrettet(arbeidsgiverrettet),
    Utvikler(utvikler),
    Ingen("");

    fun somListe(): List<String> = if (this == Ingen) emptyList() else listOf(uuid);
}

typealias EndepunktHandler = (TestRolle) -> StatusAssertions

enum class StatusType(val assertion: StatusAssertions.() -> Unit) {
    ok(StatusAssertions::isOk),
    unauthorized(StatusAssertions::isUnauthorized),
    forbidden(StatusAssertions::isForbidden),
    no_content(StatusAssertions::isNoContent),
    created(StatusAssertions::isCreated),
}

class Varianter(
    val jobbsøkerrettet: StatusType,
    val arbeidsgiverrettet: StatusType,
    val utvikler: StatusType,
    val ingen: StatusType
)


private class Kall(private val webClient: WebTestClient, private val mockLogin: MockLogin, stubber: Stubber) {

    private fun request(
        method: HttpMethod, uri: String, rolle: TestRolle, body: Any? = null
    ): StatusAssertions {
        val requestSpec = webClient.method(method).uri(uri)
            .header(
                AUTHORIZATION,
                "Bearer ${mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe())}"
            )

        return (body?.let { requestSpec.bodyValue(it) } ?: requestSpec).exchange().expectStatus()
    }

    private fun get(uri: String, rolle: TestRolle): StatusAssertions =
        request(HttpMethod.GET, uri, rolle)

    private fun put(uri: String, rolle: TestRolle, body: Any): StatusAssertions =
        request(HttpMethod.PUT, uri, rolle, body)

    private fun post(uri: String, rolle: TestRolle, body: Any? = null): StatusAssertions =
        request(HttpMethod.POST, uri, rolle, body)

    private fun delete(uri: String, rolle: TestRolle): StatusAssertions =
        request(HttpMethod.DELETE, uri, rolle)

    inner class Stilling(private val stubber: Stubber) {
        private val stillingPath = "/rekrutteringsbistandstilling"

        val opprettStilling: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.STILLING) }
        val opprettJobbmesse: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.JOBBMESSE) }
        val opprettFormidling: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.FORMIDLING) }
        val oppdaterStilling: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            val stillingsInfo = Testdata.enStillingsinfo
            stubber.mockOppdaterStilling(stilling)
            stubber.mockHentStilling(stilling)
            put(
                stillingPath,
                rolle,
                OppdaterRekrutteringsbistandStillingDto(stillingsInfo.stillingsinfoid.asString(), stilling)
            )
        }
        val kopierStilling: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            stubber.mockHentStilling(stilling)
            stubber.mockArbeidsplassenKlientOpprettStilling()
            post(
                "$stillingPath/kopier/${stilling.uuid}",
                rolle
            )
        }
        val slettStilling: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            stubber.mockSlettStilling(stilling)
            delete(
                "$stillingPath/${stilling.uuid}",
                rolle
            )
        }
        val hentStillingMedUuid: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            stubber.mockHentStilling(stilling)
            get(
                "$stillingPath/${stilling.uuid}",
                rolle
            )
        }
        val hentStillingMedAnnonsenr: EndepunktHandler = { rolle ->
            stubber.mockHentStillingMedAnnonseNr()
            get(
                "$stillingPath/annonsenr/123456",
                rolle
            )
        }
        val hentStillingForPersonBruker: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            get(
                "rekrutteringsbistand/ekstern/api/v1/stilling/${stilling.uuid}",
                rolle
            )
        }

        private fun oppRettStillingKall(rolle: TestRolle, stillingskategori: Stillingskategori): StatusAssertions {
            stubber.mockArbeidsplassenKlientOpprettStilling()
            return post(
                stillingPath,
                rolle,
                Testdata.enOpprettRekrutteringsbistandstillingDtoMedKategori(stillingskategori)
            )
        }
    }
    inner class StillingsInfo(private val stubber: Stubber) {
        private val stillingInfoPath = "/stillingsinfo"

        val overtaEierskapForEksternStillingOgKandidatliste: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            stubber.mockHentStilling(stilling)
            stubber.mockOppdaterStilling(stilling)
            put(
                stillingInfoPath,
                rolle,
                StillingsinfoInboundDto(stilling.uuid, "A123456", "Test Testesen")
            )
        }
    }
    inner class Indekser(private val stubber: Stubber) {
        private val indekserPath = "/indekser"

        val hentStillingsinfoBulk: EndepunktHandler = { rolle ->
            val stillinger = listOf(Testdata.enStilling, Testdata.enAnnenStilling)
            post(
                "$indekserPath/stillingsinfo/bulk",
                rolle,
                BulkStillingsinfoInboundDto(stillinger.map(no.nav.rekrutteringsbistand.api.stilling.Stilling::uuid))
            )
        }
    }
    inner class InnloggetVeileder(private val stubber: Stubber) {
        private val reporteePath = "/rekrutteringsbistand/api/v1/reportee"
        val hentInnloggetVeileder: EndepunktHandler = { rolle ->
            get(
                reporteePath,
                rolle
            )
        }
    }

    inner class ArbeidsplassenProxy(private val stubber: Stubber) {
        private val proxyPath = "/rekrutteringsbistand/api/v1"
        private val geografiPath = "$proxyPath/geography"
        private val searchPath = "/search-api/underenhet/_search"

        val kategorier: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockPAMAdGet("categories-with-altnames")
            get(
                "$proxyPath/categories-with-altnames",
                rolle
            )
        }
        val geografiPostnr: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockPAMAdGet("geography/postdata")
            get(
                "$geografiPath/postdata",
                rolle
            )
        }
        val geografiKommuner: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockPAMAdGet("geography/municipals")
            get(
                "$geografiPath/municipals",
                rolle
            )
        }
        val geografiFylker: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockPAMAdGet("geography/counties")
            get(
                "$geografiPath/counties",
                rolle
            )
        }
        val geografiLand: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockPAMAdGet("geography/countries")
            get(
                "$geografiPath/countries",
                rolle
            )
        }
        val getSøk: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            stubber.mockArbeidsplassenGetSearch()
            get(
                searchPath,
                rolle
            )
        }
        val postSøk: EndepunktHandler = { rolle ->
            stubber.mockArbeidsplassenPostSearch()
            post(
                searchPath,
                rolle,
                "body"
            )
        }
    }

    inner class StandardSøk(private val stubber: Stubber) {
        private val standardsokPath = "/standardsok"

        val hentStandardsøk: EndepunktHandler = { rolle ->
            stubber.lagreStandardSøk()
            get(
                standardsokPath,
                rolle
            )
        }
        val upsertStandardsøk: EndepunktHandler = { rolle ->
            stubber.mockPAMAdGet("def")
            put(
                standardsokPath,
                rolle,
                LagreStandardsøkDto("")
            )
        }
    }

    val stilling = Stilling(stubber)
    val stillingsInfo = StillingsInfo(stubber)
    val indekser = Indekser(stubber)
    val innloggetVeileder = InnloggetVeileder(stubber)
    val arbeidsplassenProxy = ArbeidsplassenProxy(stubber)
    val standardSøk = StandardSøk(stubber)
}

private class Stubber(
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val azureKlient: AzureKlient,
    private val standardsøkRepository: StandardsøkRepository
) {

    private val wireMock: WireMockServer = WireMockServer(options().port(9934))

    init {
        wireMock.start()
    }

    fun close() {
        wireMock.stop()
    }

    fun resetAll() {
        wireMock.resetAll()
    }

    fun mockArbeidsplassenKlientOpprettStilling() {
        wireMock.stubFor(
            WireMock.post(WireMock.urlPathMatching("/api/v1/ads*"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, WireMock.matching("Bearer .*"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(Testdata.enOpprettetStilling))
                )
        )
    }

    fun mockHentStilling(stilling: Stilling) {
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/b2b/api/v1/ads/${stilling.uuid}"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, WireMock.matching("Bearer .*"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(stilling))
                )
        )
    }

    fun mockHentStillingMedAnnonseNr() {
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/b2b/api/v1/ads"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, WireMock.matching("Bearer .*"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(Page(content = listOf(Testdata.enStilling), totalPages = 1, totalElements = 1)))
                )
        )
    }

    fun mockOppdaterStilling(stilling: Stilling) {
        wireMock.stubFor(
            WireMock.put(WireMock.urlPathMatching("/api/v1/ads/${stilling.uuid}"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, WireMock.matching("Bearer .*"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(stilling))
                )
        )
    }

    fun mockSlettStilling(stilling: Stilling) {
        wireMock.stubFor(
            WireMock.delete(WireMock.urlPathMatching("/api/v1/ads/${stilling.uuid}"))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, WireMock.matching("Bearer .*"))
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(HttpHeaders.CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(stilling))
                )
        )
    }

    fun mockOboToken(token: String) {
        whenever(azureKlient.hentOBOToken(anyString(), anyString(), anyString())).thenReturn(token)
    }

    fun mockArbeidsplassenGetSearch() {
        wireMock.stubFor(
            WireMock.request(HttpMethod.GET.name(), WireMock.urlPathMatching("/search-api/underenhet/_search"))
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("")
                )
        )
    }

    fun mockArbeidsplassenPostSearch() {
        wireMock.stubFor(
            WireMock.request(HttpMethod.POST.name(), WireMock.urlPathMatching("/search-api/underenhet/_search"))
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("")
                )
        )
    }

    fun mockPAMAdGet(endPath: String) {
        wireMock.stubFor(
            WireMock.request(HttpMethod.GET.name(), WireMock.urlPathMatching("/api/v1/$endPath"))
                .willReturn(
                    WireMock.aResponse().withStatus(200)
                        .withHeader(HttpHeaders.CONNECTION, "close")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("")
                )
        )
    }

    fun lagreStandardSøk() {
        standardsøkRepository.oppdaterStandardsøk(LagreStandardsøkDto("søk"), navIdent)
    }
}