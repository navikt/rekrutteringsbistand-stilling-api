package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.MatchesJsonPathPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettStillingDto
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettetStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStillingUtenEier
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoUtenEier
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = arrayOf("external.pam-ad-api.url=http://localhost:9935")
)
internal class FrontendStillingComponentTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val wiremockAzure: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9954))
            .build()

        @JvmStatic
        @RegisterExtension
        val wiremockPamAdApi: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9935).notifier(Slf4jNotifier(true)))
            .build()

        @JvmStatic
        @RegisterExtension
        val wiremockKandidatliste: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(8766))
            .build()
    }

    @MockitoBean
    lateinit var stillingssokProxyClient: StillingssokProxyClient

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var stillingsinfoRepository: StillingsinfoRepository

    @Autowired
    lateinit var direktemeldtStillingRepository: DirektemeldtStillingRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    private val objectMapper: ObjectMapper =
        ObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @BeforeEach
    fun before() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        val stilling = enStilling
        lagreDirektemeldtStillingIDatabase(stilling)

        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)

        whenever(stillingssokProxyClient.hentStilling(stilling.uuid)).thenReturn(stilling)

        restTemplate.getForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stillingsinfo).isNull()
            assertThat(it.stilling).isEqualTo(stilling.copy(versjon = 1))
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling med stillingsinfo hvis det er lagret`() {
        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))
        lagreDirektemeldtStillingIDatabase(stilling)

        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)
        whenever(stillingssokProxyClient.hentStilling(stilling.uuid)).thenReturn(stilling)

        stillingsinfoRepository.opprett(stillingsinfo)

        restTemplate.getForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stilling).isEqualTo(stilling.copy(versjon = 1))
            assertThat(it.stillingsinfo).isEqualTo(stillingsinfo.asStillingsinfoDto())
        }
    }

    @Test
    @Disabled("Denne er grønn lokalt men rød på Github")
    fun `GET mot en rekrutteringsbistandstilling skal føre til retries gitt nettverkshikke ved kall på Arbeidsplassen`() {
        val stillingsId = UUID.randomUUID().toString()
        val urlPath = "/b2b/api/v1/ads/$stillingsId"
        mockPamAdApiCorruptResponse(urlPath)
        mockAzureObo(wiremockAzure)

        val responseEntity = restTemplate.getForEntity(
            "$localBaseUrl/rekrutteringsbistandstilling/$stillingsId", String::class.java
        )

        assertTrue(responseEntity.statusCode.is5xxServerError)
        wiremockPamAdApi.verify(3, getRequestedFor(urlPathMatching(urlPath)))
    }

    @Test
    fun `Ved opprettelse av stilling skal stillingstittel i arbeidsplassen være "Ny stilling" selv om frontend ikke sender noen stillingstittel`() {
        val requestUtenStillingstittel = enOpprettRekrutteringsbistandstillingDto.copy(
            stilling = enOpprettStillingDto.copy(categoryList = emptyList())
        )

        mockPamAdApi(HttpMethod.POST, "/api/v1/ads", enOpprettetStilling.copy(title = "Ny stilling"))
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            requestUtenStillingstittel,
            RekrutteringsbistandStilling::class.java
        ).also {
            val stilling = requestUtenStillingstittel.stilling
            assertThat(it.stilling.administration?.navIdent).isEqualTo(stilling.administration.navIdent)
            assertThat(it.stilling.administration?.reportee).isEqualTo(stilling.administration.reportee)
            assertThat(it.stilling.administration?.status).isEqualTo(stilling.administration.status)
            assertThat(it.stilling.createdBy).isEqualTo(stilling.createdBy)
            assertThat(it.stilling.updatedBy).isEqualTo(stilling.updatedBy)
            assertThat(it.stilling.source).isEqualTo(stilling.source)
            assertThat(it.stilling.privacy).isEqualTo(stilling.privacy)

            assertThat(it.stillingsinfo?.stillingskategori).isEqualTo(Stillingskategori.ARBEIDSTRENING)
            assertThat(it.stillingsinfo?.eierNavKontorEnhetId).isEqualTo("1234")
        }

        wiremockPamAdApi.verify(
            1,
            RequestPatternBuilder
                .newRequestPattern(RequestMethod.POST, urlPathMatching("/api/v1/ads"))
                .withRequestBody(MatchesJsonPathPattern("title", EqualToPattern("Ny stilling")))
        )
    }

    @Test
    fun `kopiert stilling skal inneholde styrk som tittel gitt at styrk finnes i original stilling`() {
        val styrkCode = "3112.12"
        val styrkTittel = "Byggeleder"
        val styrkCodeList = listOf(Kategori(2148934, styrkCode, "STYRK08NAV", styrkTittel, null, null))
        val eksisterendeStillingMedStyrk = enStilling.copy(
            title = "Eksisterende stilling", categoryList = styrkCodeList
        )
        val eksisterendeStillingsId = UUID.randomUUID()

        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/$eksisterendeStillingsId", eksisterendeStillingMedStyrk)
        mockPamAdApi(HttpMethod.POST, "/api/v1/ads", enOpprettetStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)
        whenever(stillingssokProxyClient.hentStilling(eksisterendeStillingsId.toString())).thenReturn(eksisterendeStillingMedStyrk)

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/kopier/$eksisterendeStillingsId",
            null,
            RekrutteringsbistandStilling::class.java
        )

        wiremockPamAdApi.verify(
            1,
            RequestPatternBuilder
                .newRequestPattern(RequestMethod.POST, urlPathMatching("/api/v1/ads"))
                .withRequestBody(MatchesJsonPathPattern("title", EqualToPattern(styrkTittel)))
        )
    }

    @Test
    fun `PUT oppdaterer arbeidsplassen sin stilling med ny eier`() {
        val nyEier = "ny eier"
        val stilling = enStilling.copy(administration = Administration(null, null, null, null, emptyList(), nyEier))
        val stillingsinfo = enStillingsinfo
        stillingsinfoRepository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()
        mockPamAdApi(
            HttpMethod.GET,
            "/b2b/api/v1/ads/${stilling.uuid}",
            enStilling.copy(administration = Administration(null, null, null, null, navIdent = "Gammel"))
        )
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${stilling.uuid}", stilling)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(), stilling = stilling, stillingsinfo = stillingsinfo.asStillingsinfoDto()
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), String::class.java
        )

        wiremockPamAdApi.verify(
            1, RequestPatternBuilder
                .newRequestPattern(RequestMethod.PUT, urlPathMatching("/api/v1/ads/${stilling.uuid}"))
                .withRequestBody(MatchesJsonPathPattern("administration.navIdent", EqualToPattern(nyEier)))
        )
    }

    @Test
    fun `PUT oppdaterer direktemeldt stilling med styrk som tittel`() {
        val tittel = "Uønsket tittel"
        val source = "DIR"
        val styrkCode = "3112.12"
        val styrkTittel = "Byggeleder"
        val styrkCodeList = listOf(Kategori(2148934, styrkCode, "STYRK08NAV", styrkTittel, null, null))
        val stilling = enStilling.copy(title = tittel, source = source, categoryList = styrkCodeList)
        val stillingsinfo = enStillingsinfo
        stillingsinfoRepository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()
        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${stilling.uuid}", stilling)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = stilling,
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), String::class.java
        )

        wiremockPamAdApi.verify(
            1, RequestPatternBuilder
                .newRequestPattern(RequestMethod.PUT, urlPathMatching("/api/v1/ads/${stilling.uuid}"))
                .withRequestBody(MatchesJsonPathPattern("title", EqualToPattern(styrkTittel)))
                .withRequestBody(MatchesJsonPathPattern("categoryList[0].code", EqualToPattern(styrkCode)))
                .withRequestBody(MatchesJsonPathPattern("categoryList[0].name", EqualToPattern(styrkTittel)))
        )
    }

    @Test
    fun `PUT mot stilling skal returnere 500 og ikke gjøre endringer i databasen når kall mot Arbeidsplassen feiler`() {
        val stilling = enStilling
        val stillingsinfo = enStillingsinfo
        stillingsinfoRepository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()
        mockPamAdApi(
            HttpMethod.GET,
            "/b2b/api/v1/ads/${stilling.uuid}",
            enStilling.copy(administration = Administration(null, null, null, null, navIdent = "Gammel"))
        )
        mockPamAdApiError("/api/v1/ads/${stilling.uuid}", HttpMethod.PUT, 500)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = stilling,
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), String::class.java
        ).also {
            assertThat(it.statusCode.value()).isEqualTo(500)
        }
    }

    @Test
    fun `PUT mot stilling skal returnere 500 og ikke gjøre endringer i database når kall mot kandidat-api feiler`() {
        val stilling = enOpprettetStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))
        stillingsinfoRepository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${stilling.uuid}", stilling)
        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/${stilling.uuid}", enStilling)
        mockKandidatlisteOppdateringFeiler()

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = stilling,
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        direktemeldtStillingRepository.lagreDirektemeldtStilling(DirektemeldtStilling(
            stillingsId = UUID.fromString(stilling.uuid),
            innhold = stilling.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.of(stilling.created, ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndret = ZonedDateTime.of(stilling.updated, ZoneId.of("Europe/Oslo")),
            sistEndretAv = stilling.updatedBy,
            status = stilling.status,
            annonsenr = stilling.annonsenr,
            versjon = stilling.versjon ?: 1,
            utløpsdato = ZonedDateTime.of(stilling.expires, ZoneId.of("Europe/Oslo")),
            publisert = ZonedDateTime.of(stilling.published, ZoneId.of("Europe/Oslo")),
            publisertAvAdmin = stilling.publishedByAdmin,
            adminStatus = stilling.administration?.status
        ))

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), String::class.java
        ).also {
            assertThat(it.statusCode.value()).isEqualTo(500)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo finnes`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStilling
        val stillingsinfo = enStillingsinfo

        lagreDirektemeldtStillingIDatabase(rekrutteringsbistandStilling.stilling)

        mockPamAdApi(
            HttpMethod.PUT,
            "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}",
            rekrutteringsbistandStilling.stilling
        )
        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", enStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        stillingsinfoRepository.opprett(stillingsinfo)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = rekrutteringsbistandStilling.stilling.copy(versjon = 1),
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling",
            HttpMethod.PUT,
            HttpEntity(dto),
            OppdaterRekrutteringsbistandStillingDto::class.java
        ).body!!.also {
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling.copy(updated = it.stilling.updated, versjon = 2)) // ignorer id og updated
            assertThat(it.stillingsinfoid).isEqualTo(stillingsinfo.stillingsinfoid.asString())
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke har eier`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStillingUtenEier
        lagreDirektemeldtStillingIDatabase(rekrutteringsbistandStilling.stilling)

        mockPamAdApi(
            HttpMethod.PUT,
            "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}",
            rekrutteringsbistandStilling.stilling
        )
        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", enStilling)
        mockAzureObo(wiremockAzure)

        mockKandidatlisteOppdatering()
        stillingsinfoRepository.opprett(enStillingsinfoUtenEier)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(
                OppdaterRekrutteringsbistandStillingDto(
                    stillingsinfoid = rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid,
                    stilling = rekrutteringsbistandStilling.stilling.copy(versjon = 1),
                    stillingsinfo = null,
                )
            ), OppdaterRekrutteringsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling.copy(updated = it.stilling.updated, versjon = 2)) // ignorer id og updated
            assertThat(it.stillingsinfoid).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid)
        }
    }

    @Test
    fun `DELETE mot stillinger skal slette stilling og returnere 200`() {
        val slettetStilling = enStilling.copy(status = "DELETED")
        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteSlettet()
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${slettetStilling.uuid}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val stilling = it.body
            assertThat(stilling?.title).isEqualTo(slettetStilling.title)
            assertThat(stilling?.administration?.navIdent).isEqualTo(slettetStilling.administration?.navIdent)
            assertThat(stilling?.administration?.reportee).isEqualTo(slettetStilling.administration?.reportee)
            assertThat(stilling?.administration?.status).isEqualTo(slettetStilling.administration?.status)
            assertThat(stilling?.createdBy).isEqualTo(slettetStilling.createdBy)
            assertThat(stilling?.updatedBy).isEqualTo(slettetStilling.updatedBy)
            assertThat(stilling?.source).isEqualTo(slettetStilling.source)
            assertThat(stilling?.privacy).isEqualTo(slettetStilling.privacy)
            assertThat(stilling?.status).isEqualTo("DELETED")
            assertThat(it.statusCode).isEqualTo(OK)
        }
    }

    @Test
    fun `DELETE mot stillinger skal slette stilling og returnere 200 også når kandidatliste ikke er opprettet for stilling`() {
        val slettetStilling = enStilling.copy(status = "DELETED")
        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteIkkeSlettetFordiDenIkkeEksisterer()
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${slettetStilling.uuid}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val stilling = it.body
            assertThat(stilling?.title).isEqualTo(slettetStilling.title)
            assertThat(stilling?.administration?.navIdent).isEqualTo(slettetStilling.administration?.navIdent)
            assertThat(stilling?.administration?.reportee).isEqualTo(slettetStilling.administration?.reportee)
            assertThat(stilling?.administration?.status).isEqualTo(slettetStilling.administration?.status)
            assertThat(stilling?.createdBy).isEqualTo(slettetStilling.createdBy)
            assertThat(stilling?.updatedBy).isEqualTo(slettetStilling.updatedBy)
            assertThat(stilling?.source).isEqualTo(slettetStilling.source)
            assertThat(stilling?.privacy).isEqualTo(slettetStilling.privacy)
            assertThat(stilling?.status).isEqualTo("DELETED")
            assertThat(it.statusCode).isEqualTo(OK)
        }
    }

    @Test
    fun `DELETE mot stilling med kandidatlistefeil skal returnere status 500`() {
        val slettetStilling = enStilling.copy(status = "DELETED")

        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockFeilendeKallTilKandidatApiForSlettingAvStilling()
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${enStilling.uuid}",
            HttpMethod.DELETE,
            null,
            FrontendStilling::class.java
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @Test
    fun `Sletting av stilling skal ikke slette tilhørende Stillingsinfo`() {
        // Gitt en stilling med en Stillingsinfo
        val stilling = enStilling
        val stillingsId = Stillingsid(stilling.uuid)
        val stillingsinfo = enStillingsinfo.copy(stillingsid = stillingsId)
        stillingsinfoRepository.opprett(stillingsinfo)
        val slettetStilling = stilling.copy(status = "DELETED")
        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteSlettet()
        mockAzureObo(wiremockAzure)
        stillingsinfoRepository.hentForStilling(stillingsId) ?: fail("Setup")

        // når vi sletter stillingen
        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${slettetStilling.uuid}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val stillingIRespons = it.body!!
            assertThat(it.statusCode == OK)
            assertThat(stillingIRespons.status).isEqualTo("DELETED")
        }

        // så skal stillingens Stillingsinfo ikke slettes
        stillingsinfoRepository.hentForStilling(stillingsId) ?: fail("Det skal finnes en Stillingsinfo i db for ")

    }

    private fun mockPamAdApi(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            request(method.name(), urlPathMatching(urlPath)).withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(
                    aResponse().withStatus(200).withHeader(
                        CONNECTION, "close"
                    ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )
    }

    private fun mockPamAdApiError(
        urlPath: String, method: HttpMethod = HttpMethod.GET, httpResponseStatus: Int = 418
    ) {
        wiremockPamAdApi.stubFor(
            request(method.name(), urlPathMatching(urlPath)).withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(aResponse().withStatus(httpResponseStatus))
        )
    }

    private fun mockPamAdApiCorruptResponse(
        urlPath: String, method: HttpMethod = HttpMethod.GET, fault: Fault = CONNECTION_RESET_BY_PEER
    ) {
        wiremockPamAdApi.stubFor(
            request(method.name(), urlPathMatching(urlPath)).withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(aResponse().withFault(fault))
        )
    }

    private fun mockUtenAuthorization(urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            get(urlEqualTo(urlPath)).withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).willReturn(
                    aResponse().withStatus(200).withHeader(
                        CONNECTION, "close"
                    ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )
    }

    private fun mockKandidatlisteOppdatering(metodeFunksjon: (UrlPattern) -> MappingBuilder = ::put) {
        wiremockKandidatliste.stubFor(
            metodeFunksjon(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/kandidatliste")).withHeader(
                CONTENT_TYPE,
                equalTo(APPLICATION_JSON_VALUE)
            ).withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).willReturn(
                aResponse().withStatus(HttpStatus.NO_CONTENT.value()).withHeader(
                    CONNECTION, "close"
                ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun mockKandidatlisteSlettet() {
        wiremockKandidatliste.stubFor(
            delete(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste")).withHeader(
                CONTENT_TYPE,
                equalTo(APPLICATION_JSON_VALUE)
            ).withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).willReturn(
                aResponse().withStatus(HttpStatus.NO_CONTENT.value()).withHeader(
                    CONNECTION, "close"
                ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun mockKandidatlisteIkkeSlettetFordiDenIkkeEksisterer() {
        wiremockKandidatliste.stubFor(
            delete(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste")).withHeader(
                CONTENT_TYPE,
                equalTo(APPLICATION_JSON_VALUE)
            ).withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).willReturn(
                aResponse().withStatus(HttpStatus.NOT_FOUND.value()).withHeader(
                    CONNECTION, "close"
                ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun mockKandidatlisteOppdateringFeiler() {
        wiremockKandidatliste.stubFor(
            put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/kandidatliste"))
                .withHeader(
                    CONTENT_TYPE,
                    equalTo(APPLICATION_JSON_VALUE)
                )
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(
                    aResponse().withStatus(500).withHeader(
                        CONNECTION, "close"
                    ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                )
        )
    }

    private fun mockFeilendeKallTilKandidatApiForSlettingAvStilling() {
        wiremockKandidatliste.stubFor(
            delete(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.+/kandidatliste")).withHeader(
                CONTENT_TYPE,
                equalTo(APPLICATION_JSON_VALUE)
            ).withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE)).willReturn(
                aResponse().withStatus(500).withHeader(
                    CONNECTION, "close"
                ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
            )
        )
    }

    private fun lagreDirektemeldtStillingIDatabase(stilling: FrontendStilling) {
        direktemeldtStillingRepository.lagreDirektemeldtStilling(DirektemeldtStilling(
            stillingsId = UUID.fromString(stilling.uuid),
            innhold = stilling.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.of(stilling.created, ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndret = ZonedDateTime.of(stilling.updated, ZoneId.of("Europe/Oslo")),
            sistEndretAv = stilling.updatedBy,
            status = stilling.status,
            annonsenr = stilling.annonsenr,
            utløpsdato = ZonedDateTime.of(stilling.expires, ZoneId.of("Europe/Oslo")),
            publisert = ZonedDateTime.of(stilling.published, ZoneId.of("Europe/Oslo")),
            publisertAvAdmin = stilling.publishedByAdmin,
            adminStatus = stilling.administration?.status
        ))
    }


    @AfterEach
    fun after() {
        testRepository.slettAlt()
    }

}
