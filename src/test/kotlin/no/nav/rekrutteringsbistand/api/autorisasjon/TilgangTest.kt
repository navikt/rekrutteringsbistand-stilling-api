package no.nav.rekrutteringsbistand.api.autorisasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.*
import io.ktor.utils.io.core.*
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ProxyTilArbeidsplassen
import no.nav.rekrutteringsbistand.api.autorisasjon.StatusType.*
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.config.arbeidsgiverrettet
import no.nav.rekrutteringsbistand.api.config.jobbsøkerrettet
import no.nav.rekrutteringsbistand.api.config.utvikler
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.rekrutteringsbistand.api.stillingsinfo.indekser.BulkStillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.StatusAssertions
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

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
    private lateinit var arbeidsplassenKlient: ArbeidsplassenKlient

    @MockBean
    private lateinit var kandidatlisteKlient: KandidatlisteKlient

    @MockBean
    private lateinit var azureKlient: AzureKlient

    @MockBean
    private lateinit var stillingService: StillingService

    private lateinit var stubber: Stubber

    private val restTemplate = TestRestTemplate()

    private fun localBaseUrl(): String = "http://localhost:$port"

    @BeforeAll
    fun setup() {
        stubber = Stubber(arbeidsplassenKlient, kandidatlisteKlient, azureKlient)
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
                    "NAVident" to "C12345",
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
            stilling::kopierStilling to Uimplementert,
            stilling::slettStilling to Uimplementert,
            stilling::hentStillingMedUuid to Uimplementert,
            stilling::hentStillingMedAnnonsenr to Uimplementert,
            stilling::hentStillingForPersonBruker to Uimplementert,
            stillingsInfo::overtaEierskapForEksternStillingOgKandidatliste to Uimplementert,
            indekser::hentStillingsinfoBulk to Uimplementert,
            innloggetVeileder::hentInnloggetVeileder to Uimplementert,
            arbeidsplassenProxy::kategorier to Uimplementert,
            arbeidsplassenProxy::geografiPostnr to Uimplementert,
            arbeidsplassenProxy::geografiKommuner to Uimplementert,
            arbeidsplassenProxy::geografiFylker to Uimplementert,
            arbeidsplassenProxy::geografiLand to Uimplementert,
            arbeidsplassenProxy::getSøk to Uimplementert,
            arbeidsplassenProxy::postSøk to Uimplementert,
            standardSøk::hentStandardsøk to Uimplementert,
            standardSøk::upsertStandardsøk to Uimplementert
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
    forbidden(StatusAssertions::isForbidden),
    no_content(StatusAssertions::isNoContent),
    created(StatusAssertions::isCreated),
    todo({TODO()})
}

class Varianter(
    val jobbsøkerrettet: StatusType,
    val arbeidsgiverrettet: StatusType,
    val utvikler: StatusType,
    val ingen: StatusType
)

val Uimplementert = Varianter(todo,todo,todo,todo)

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
            put(
                stillingPath,
                rolle,
                OppdaterRekrutteringsbistandStillingDto(stillingsInfo.stillingsinfoid.asString(), stilling)
            )
        }
        val kopierStilling: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            post(
                "$stillingPath/kopier/${stilling.uuid}",
                rolle
            )
        }
        val slettStilling: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            delete(
                "$stillingPath/${stilling.uuid}",
                rolle
            )
        }
        val hentStillingMedUuid: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            get(
                "$stillingPath/${stilling.uuid}",
                rolle
            )
        }
        val hentStillingMedAnnonsenr: EndepunktHandler = { rolle ->
            get(
                "$stillingPath/annonsenr/PAM012345",
                rolle
            )
        }
        val hentStillingForPersonBruker: EndepunktHandler = { rolle ->
            val stilling = Testdata.enStilling
            get(
                "$stillingPath/ekstern/api/v1/stilling/${stilling.uuid}",
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
            stubber.mockStilling(stilling)
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
            get(
                "$proxyPath/categories-with-altnames",
                rolle
            )
        }
        val geografiPostnr: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            get(
                "$geografiPath/postdata",
                rolle
            )
        }
        val geografiKommuner: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            get(
                "$geografiPath/municipals",
                rolle
            )
        }
        val geografiFylker: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            get(
                "$geografiPath/counties",
                rolle
            )
        }
        val geografiLand: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            get(
                "$geografiPath/countries",
                rolle
            )
        }
        val getSøk: EndepunktHandler = { rolle ->
            stubber.mockOboToken(mockLogin.hentAzureAdVeilederToken(roller = rolle.somListe()))
            get(
                searchPath,
                rolle
            )
        }
        val postSøk: EndepunktHandler = { rolle ->
            post(
                searchPath,
                rolle,
                ""
            )
        }
    }

    inner class StandardSøk(private val stubber: Stubber) {
        private val standardsokPath = "/standardsok"

        val hentStandardsøk: EndepunktHandler = { rolle ->
            get(
                standardsokPath,
                rolle
            )
        }
        val upsertStandardsøk: EndepunktHandler = { rolle ->
            put(
                standardsokPath,
                rolle,
                ""
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
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val azureKlient: AzureKlient
) {

    private val wireMock: WireMockServer = WireMockServer(options().port(9089))
    private val kandidatSokWireMock: WireMockServer = WireMockServer(options().port(9090))

    init {
        wireMock.start()
        kandidatSokWireMock.start()
    }

    fun close() {
        wireMock.stop()
        kandidatSokWireMock.stop()
    }

    fun resetAll() {
        wireMock.resetAll()
        kandidatSokWireMock.resetAll()
    }

    fun mockArbeidsplassenKlientOpprettStilling() {
        whenever(arbeidsplassenKlient.opprettStilling(any<OpprettStillingDto>())).thenReturn(Testdata.enStilling)
    }

    fun mockKandidatlisteKlientSendStillingOppdatert() {
        whenever(kandidatlisteKlient.sendStillingOppdatert(any<RekrutteringsbistandStilling>())).thenReturn(ResponseEntity.of(Optional.empty()))
    }

    fun mockStilling(stilling: Stilling) {
        whenever(arbeidsplassenKlient.hentStilling(eq(stilling.uuid), anyBoolean())).thenReturn(stilling)
    }

    fun mockOboToken(token: String) {
        whenever(azureKlient.hentOBOToken(anyString(), anyString(), anyString())).thenReturn(token)
    }
}