package no.nav.rekrutteringsbistand.api.autorisasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.ktor.http.*
import io.ktor.utils.io.core.*
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.StatusType.forbidden
import no.nav.rekrutteringsbistand.api.autorisasjon.StatusType.ok
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.config.arbeidsgiverrettet
import no.nav.rekrutteringsbistand.api.config.jobbsøkerrettet
import no.nav.rekrutteringsbistand.api.config.utvikler
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
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
import org.mockito.ArgumentMatchers.anyString
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
    private lateinit var stillingService: StillingService

    private lateinit var stubber: Stubber

    private val restTemplate = TestRestTemplate()

    private fun localBaseUrl(): String = "http://localhost:$port"

    @BeforeAll
    fun setup() {
        stubber = Stubber(arbeidsplassenKlient, kandidatlisteKlient)
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
    created(StatusAssertions::isCreated)
}

class Varianter(
    val jobbsøkerrettet: StatusType,
    val arbeidsgiverrettet: StatusType,
    val utvikler: StatusType,
    val ingen: StatusType
)
private class Kall(private val webClient: WebTestClient, private val mockLogin: MockLogin, stubber: Stubber) {

    private val stillingPath = "/rekrutteringsbistandstilling"

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

    private fun post(uri: String, rolle: TestRolle, body: Any): StatusAssertions =
        request(HttpMethod.POST, uri, rolle, body)

    private fun delete(uri: String, rolle: TestRolle): StatusAssertions =
        request(HttpMethod.DELETE, uri, rolle)

    inner class Stilling(private val stubber: Stubber) {

        val opprettStilling: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.STILLING) }
        val opprettJobbmesse: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.JOBBMESSE) }
        val opprettFormidling: EndepunktHandler = { rolle -> oppRettStillingKall(rolle, Stillingskategori.FORMIDLING) }

        private fun oppRettStillingKall(rolle: TestRolle, stillingskategori: Stillingskategori): StatusAssertions {
            stubber.mockArbeidsplassenKlientOpprettStilling()
            return post(
                stillingPath,
                rolle,
                Testdata.enOpprettRekrutteringsbistandstillingDtoMedKategori(stillingskategori)
            )
        }
    }

    val stilling = Stilling(stubber)
}

private class Stubber(
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
    private val kandidatlisteKlient: KandidatlisteKlient
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
}
/*
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestInstance(PER_CLASS)
class TilgangTest {

    @Autowired
    private lateinit var kandidatlisteRepository: KandidatlisteRepository

    @Autowired
    private lateinit var esSokServiceMock: EsSokService

    @MockBean
    private lateinit var azureADKlient: AzureADKlient

    @MockBean
    private lateinit var stsKlient: StsKlient

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var mockLogin: MockLogin

    private lateinit var stubber: Stubber

    @BeforeAll
    fun setup() {
        stubber = Stubber(esSokServiceMock, kandidatlisteRepository)
    }

    @BeforeEach
    fun beforeEach() {
        kandidatlisteRepository.deleteAll()
        `when`(azureADKlient.onBehalfOfToken(anyString(), anyString())).thenReturn("true")
        `when`(stsKlient.getToken()).thenReturn(StsToken("", "", 60))
        stubber.resetAll()
    }

    @AfterAll
    fun afterAll() {
        stubber.close()
    }

    fun tilgangsTester() = Kall(webClient, mockLogin, stubber).run {
        listOf(
            status::hentIsAlive to Varianter(ok, ok, ok, ok),
            status::hentIsReady to Varianter(ok, ok, ok, ok),

            maskinbruker::hentKandidatlisteIdForStillingMaskinbruker to
                    Varianter(forbidden, forbidden, forbidden, forbidden),
            maskinbruker::hentKandidatlisteMaskinbruker to Varianter(forbidden, forbidden, forbidden, forbidden),

            usynligeKandidater::hentNavn to Varianter(ok, ok, ok, forbidden),
            usynligeKandidater::leggTilFormidlingsutfallForUsynligKandidat to
                    Varianter(forbidden, created, created, forbidden),
            usynligeKandidater::leggTilFormidlingsutfallForUsynligKandidat to
                    Varianter(created, created, created, forbidden, Input(FORMIDLING)),
            usynligeKandidater::endreFormidlingsutfallForUsynligKandidat to Varianter(forbidden, ok, ok, forbidden),
            usynligeKandidater::endreFormidlingsutfallForUsynligKandidat to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING)),

            kandidat::visListerKandidatenErI to Varianter(ok, ok, ok, forbidden),

            kandidatliste::hentKandidatlistePåStillingsId to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::hentKandidatlistePåStillingsId to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING)),
            kandidatliste::hentKandidatlistePåStillingsId to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::opprettKandidatlisteBasertPåStilling to
                    Varianter(forbidden, no_content, no_content, forbidden),
            kandidatliste::opprettKandidatlisteBasertPåStilling to
                    Varianter(no_content, no_content, no_content, forbidden, Input(FORMIDLING)),
            kandidatliste::oppdaterKandidatlisteBasertPåStilling to
                    Varianter(forbidden, no_content, no_content, forbidden),
            kandidatliste::oppdaterKandidatlisteBasertPåStilling to
                    Varianter(forbidden, no_content, no_content, forbidden, Input(FORMIDLING)),
            kandidatliste::oppdaterKandidatlisteEierPåStilling to
                    Varianter(forbidden, no_content, no_content, forbidden),
            kandidatliste::oppdaterKandidatlisteStatusPåStilling to
                    Varianter(forbidden, no_content, no_content, forbidden),
            kandidatliste::oppdaterKandidatlisteStatusPåStilling to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::slettKandidatlisteBasertPåStillingsId to
                    Varianter(forbidden, no_content, no_content, forbidden),
            kandidatliste::slettKandidatlisteBasertPåStillingsId to
                    Varianter(no_content, no_content, no_content, forbidden, Input(FORMIDLING)),
            kandidatliste::slettKandidatlisteBasertPåStillingsId to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::sokEtterKandidatlistene to Varianter(ok, ok, ok, forbidden),
            kandidatliste::hentKandidatliste to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::hentKandidatliste to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING)),
            kandidatliste::hentKandidatliste to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::hentKandidatlisteIdBasertPåStillingsId to Varianter(ok, ok, ok, ok),
            kandidatliste::hentAntallKandidaterIKandidatliste to Varianter(ok, ok, ok, ok),
            kandidatliste::delKandidaterMedArbeidsgiver to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::delKandidaterMedArbeidsgiver to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::LeggTilKandidaterIKandidatliste to Varianter(created, created, created, forbidden),
            kandidatliste::SlettKandidatFraArbeidsgiversKandidatliste to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::SlettKandidatFraArbeidsgiversKandidatliste to
                    Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::settStatus to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::settStatus to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING)),
            kandidatliste::settStatus to Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::settUtfall to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::settUtfall to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING)),
            kandidatliste::settUtfall to Varianter(forbidden, forbidden, forbidden, forbidden, Input(STILLING, enAnnenVeileder)),
            kandidatliste::toggleArkivert to Varianter(forbidden, ok, ok, forbidden),
            kandidatliste::toggleArkivert to Varianter(ok, ok, ok, forbidden, Input(FORMIDLING))

        ).flatMap { (kall, svar) ->
            listOf(
                of(kall.name, Rolle.Jobbsøkerrettet, svar.jobbsøkerrettet, svar.input, kall()),
                of(kall.name, Rolle.Arbeidsgiverrettet, svar.arbeidsgiverrettet, svar.input, kall()),
                of(kall.name, Rolle.Utvikler, svar.utvikler, svar.input, kall()),
                of(kall.name, Rolle.Ingen, svar.ingen, svar.input, kall()),
            )
        }
    }

    @ParameterizedTest(name = "{index}: {0} med rolle {1} og kategori {3} skal returnere {2}")
    @MethodSource("tilgangsTester")
    fun Tilgangstest(
        kallnavnBruktTilTestBeskrivelse: String,
        rolle: Rolle,
        statusType: StatusType,
        input: Input,
        kallFunksjon: EndepunktHandler,
    ) {
        val innloggetVeilder = enVeileder

        var kandidatliste = veiledersKandidatlisteMedArbeidsgiversKandidat(input.veileder, input.kategori)

        kandidatlisteRepository.save(kandidatliste)
        statusType.assertion(kallFunksjon(rolle, innloggetVeilder, kandidatliste, input))
    }
}

typealias EndepunktHandler = (Rolle, Veileder, VeiledersKandidatliste, Input) -> StatusAssertions

data class Input(
    val kategori: Stillingskategori = STILLING,
    val veileder: Veileder = enVeileder
)



private class Stubber(
    private val esSokServiceMock: EsSokService,
    private val kandidatlisteRepository: KandidatlisteRepository
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

    // Kandidatlistemocks
    fun esVeilederHentKandidater() {
        `when`(esSokServiceMock.veilederHentKandidater(any())).thenReturn(
            Sokeresultat(2, listOf(enEsCv("Aktor01016012345", "CV51953")), emptyList())
        )
    }

    fun esVeilederHent() {
        `when`(esSokServiceMock.veilederHent(any())).thenReturn(
            Optional.of(enEsCv("Aktor01016012345", "CV51953"))
        )
    }

    fun pdlHentPerson() {
        wireMock.stubFor(
            WireMock.post("/graphql")
                .willReturn(
                    WireMock.jsonResponse(
                        PdlRespons(
                            Data(
                                HentPerson(
                                    listOf(
                                        Navn(
                                            "Fornavn",
                                            "Mellom",
                                            "Etternavn",
                                            null,
                                            Metadata("", emptyList())
                                        )
                                    ),
                                    listOf(Adressebeskyttelse(Gradering.UGRADERT))
                                ),
                                HentIdenter(
                                    listOf(
                                        Identinformasjon(
                                            "12345678910",
                                            IdentGruppe.FOLKEREGISTERIDENT,
                                            false
                                        ),
                                        Identinformasjon(
                                            "10000000001",
                                            IdentGruppe.AKTORID,
                                            false
                                        )
                                    )
                                )
                            ),
                            emptyList()
                        ), 200
                    )
                )
        )
    }

    fun forespørsler() {
        wireMock.stubFor(
            WireMock.get(WireMock.urlPathMatching("/foresporsler/.*")).willReturn(
                WireMock.aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(
                    asJson(forespørslerPåToStillinger)
                )
            )
        )
    }

    fun leggTilFormidlingAvUsynligKandidat(kandidatliste: VeiledersKandidatliste): Long {
        kandidatliste.leggTilFormidlingAvUsynligKandidat(
            FormidlingAvUsynligKandidat(
                "12345678910",
                "",
                "",
                "",
                "",
                KandidatUtfall.PRESENTERT,
                "",
                "",
                LocalDateTime.now()
            )
        )
        kandidatlisteRepository.save(kandidatliste)
        return kandidatlisteRepository.findByKandidatlisteId(kandidatliste.kandidatlisteId)
            .get().formidlingerAvUsynligKandidat.first().dbId
    }

    fun kandidatSokBrukerTilgang(kandidater: MutableList<Kandidat>) {
        kandidater.forEach {
            kandidatSokWireMock.stubFor(
                WireMock.post(WireMock.urlPathEqualTo("/api/brukertilgang"))
                    .withRequestBody(
                        WireMock.matchingJsonPath(
                            "kandidatnr",
                            WireMock.equalTo(it.kandidatnr.asString())
                        )
                    )
                    .willReturn(
                        WireMock.aResponse()
                            .withStatus(200)
                            .withBody("")
                    )
            )
        }
    }
}



*/