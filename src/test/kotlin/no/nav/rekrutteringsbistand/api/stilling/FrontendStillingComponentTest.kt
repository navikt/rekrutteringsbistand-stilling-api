package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata
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
        val wiremockKandidatliste: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(8766))
            .build()
    }

    @Autowired
    private lateinit var direktemeldtStillingRepository: DirektemeldtStillingRepository

    @MockitoBean
    lateinit var stillingssokProxyClient: StillingssokProxyClient

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

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
        mockAzureObo(wiremockAzure)

        whenever(stillingssokProxyClient.hentStilling(stilling.uuid)).thenReturn(stilling)

        restTemplate.getForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stillingsinfo).isNull()
            assertThat(it.stilling).isEqualTo(stilling)
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling med stillingsinfo hvis det er lagret`() {
        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))

        mockAzureObo(wiremockAzure)
        whenever(stillingssokProxyClient.hentStilling(stilling.uuid)).thenReturn(stilling)

        repository.opprett(stillingsinfo)

        restTemplate.getForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stilling).isEqualTo(stilling)
            assertThat(it.stillingsinfo).isEqualTo(stillingsinfo.asStillingsinfoDto())
        }
    }

    @Test
    @Disabled("Denne er grønn lokalt men rød på Github")
    fun `GET mot en rekrutteringsbistandstilling skal føre til retries gitt nettverkshikke ved kall på Arbeidsplassen`() {
        val stillingsId = UUID.randomUUID().toString()
        mockAzureObo(wiremockAzure)

        val responseEntity = restTemplate.getForEntity(
            "$localBaseUrl/rekrutteringsbistandstilling/$stillingsId", String::class.java
        )

        assertTrue(responseEntity.statusCode.is5xxServerError)
    }

    @Test
    fun `Ved opprettelse av stilling skal stillingstittel i arbeidsplassen være "Ny stilling" selv om frontend ikke sender noen stillingstittel`() {
        val requestUtenStillingstittel = enOpprettRekrutteringsbistandstillingDto.copy(
            stilling = enOpprettStillingDto.copy(categoryList = emptyList())
        )

        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            requestUtenStillingstittel,
            RekrutteringsbistandStilling::class.java
        ).also {
            val stilling = requestUtenStillingstittel.stilling
            assertThat(it.stilling.title).isEqualTo("Ny stilling")
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
    }

    @Test
    fun `kopiert stilling skal inneholde styrk som tittel gitt at styrk finnes i original stilling`() {
        val styrkCode = "3112.12"
        val styrkTittel = "Byggeleder"
        val styrkCodeList = listOf(DirektemeldtStillingKategori(code = styrkCode, categoryType = "STYRK08NAV", name = styrkTittel, description = "", parentId = null))
        val eksisterendeStillingMedStyrk = Testdata.enDirektemeldtStilling.copy(innhold = Testdata.enDirektemeldtStilling.innhold.copy(
            title = "Eksisterende stilling", categoryList = styrkCodeList
        ))

        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        direktemeldtStillingRepository.lagreDirektemeldtStilling(eksisterendeStillingMedStyrk)

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling/kopier/${eksisterendeStillingMedStyrk.stillingsId}",
            null,
            RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stilling.title).isEqualTo(styrkTittel)
        }
    }

    @Test
    fun `PUT oppdaterer stilling med ny eier`() {
        val nyEier = "ny eier"
        val stilling = Testdata.enDirektemeldtStilling

        direktemeldtStillingRepository.lagreDirektemeldtStilling(stilling)

        val stillingsinfo = enStillingsinfo
        repository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val oppdatertStilling = stilling.toStilling().copy(
            administration = stilling.toStilling().administration?.copy(navIdent = nyEier)
        )

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(), stilling = oppdatertStilling, stillingsinfo = stillingsinfo.asStillingsinfoDto()
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), OppdaterRekrutteringsbistandStillingDto::class.java
        ).also {
            assertThat(it.body?.stilling?.administration?.navIdent).isEqualTo(nyEier)
        }
    }

    @Test
    fun `PUT oppdaterer direktemeldt stilling med styrk som tittel`() {
        val tittel = "Uønsket tittel"
        val styrkCode = "3112.12"
        val styrkTittel = "Byggeleder"

        val styrkCodeList = listOf(DirektemeldtStillingKategori(code = styrkCode, categoryType = "STYRK08NAV", name = styrkTittel, description = "", parentId = null))
        val stilling = Testdata.enDirektemeldtStilling.copy(innhold = Testdata.enDirektemeldtStilling.innhold.copy(
            title = tittel, categoryList = styrkCodeList
        ))

        val stillingsinfo = enStillingsinfo
        repository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = stilling.toStilling(),
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(dto), OppdaterRekrutteringsbistandStillingDto::class.java
        ).also {
            assertThat(it.body?.stilling?.title).isEqualTo(styrkTittel)
            assertThat(it.body?.stilling?.categoryList[0]?.code).isEqualTo(styrkCode)
            assertThat(it.body?.stilling?.categoryList[0]?.name).isEqualTo(styrkTittel)
        }
    }

    @Test
    fun `PUT mot stilling skal returnere 500 og ikke gjøre endringer i database når kall mot kandidat-api feiler`() {
        val stilling = enOpprettetStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))
        repository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdateringFeiler()

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
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo finnes`() {

        val rekrutteringsbistandStilling = enRekrutteringsbistandStilling
        val stillingsinfo = enStillingsinfo

        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        repository.opprett(stillingsinfo)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            stilling = rekrutteringsbistandStilling.stilling,
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling",
            HttpMethod.PUT,
            HttpEntity(dto),
            OppdaterRekrutteringsbistandStillingDto::class.java
        ).body!!.also {
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling.copy(updated = it.stilling.updated)) // ignorer id og updated
            assertThat(it.stillingsinfoid).isEqualTo(stillingsinfo.stillingsinfoid.asString())
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke har eier`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStillingUtenEier

        mockAzureObo(wiremockAzure)

        mockKandidatlisteOppdatering()
        repository.opprett(enStillingsinfoUtenEier)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling", HttpMethod.PUT, HttpEntity(
                OppdaterRekrutteringsbistandStillingDto(
                    stillingsinfoid = rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid,
                    stilling = rekrutteringsbistandStilling.stilling,
                    stillingsinfo = null,
                )
            ), OppdaterRekrutteringsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling.copy(updated = it.stilling.updated)) // ignorer id og updated
            assertThat(it.stillingsinfoid).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid)
        }
    }

    @Test
    fun `DELETE mot stillinger skal slette stilling og returnere 200`() {

        val stilling = Testdata.enDirektemeldtStilling
        direktemeldtStillingRepository.lagreDirektemeldtStilling(stilling)

        mockKandidatlisteSlettet()
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.stillingsId}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val result = it.body
            assertThat(result?.title).isEqualTo(stilling.innhold.title)
            assertThat(result?.administration?.navIdent).isEqualTo(stilling.innhold.administration?.navIdent)
            assertThat(result?.administration?.reportee).isEqualTo(stilling.innhold.administration?.reportee)
            assertThat(result?.administration?.status).isEqualTo("DONE")
            assertThat(result?.createdBy).isEqualTo(stilling.opprettetAv)
            assertThat(result?.updatedBy).isEqualTo(stilling.sistEndretAv)
            assertThat(result?.source).isEqualTo(stilling.innhold.source)
            assertThat(result?.privacy).isEqualTo(stilling.innhold.privacy)
            assertThat(result?.status).isEqualTo("DELETED")
            assertThat(it.statusCode).isEqualTo(OK)
        }
    }

    @Test
    fun `DELETE mot stillinger skal slette stilling og returnere 200 også når kandidatliste ikke er opprettet for stilling`() {

        val stilling = Testdata.enDirektemeldtStilling
        direktemeldtStillingRepository.lagreDirektemeldtStilling(stilling)

        mockKandidatlisteIkkeSlettetFordiDenIkkeEksisterer()
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${stilling.stillingsId}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val stillingSlettet = it.body
            assertThat(stillingSlettet?.title).isEqualTo(stilling.innhold.title)
            assertThat(stillingSlettet?.administration?.navIdent).isEqualTo(stilling.innhold.administration?.navIdent)
            assertThat(stillingSlettet?.administration?.reportee).isEqualTo(stilling.innhold.administration?.reportee)
            assertThat(stillingSlettet?.administration?.status).isEqualTo("DONE")
            assertThat(stillingSlettet?.createdBy).isEqualTo(stilling.opprettetAv)
            assertThat(stillingSlettet?.updatedBy).isEqualTo(stilling.sistEndretAv)
            assertThat(stillingSlettet?.source).isEqualTo(stilling.innhold.source)
            assertThat(stillingSlettet?.privacy).isEqualTo(stilling.innhold.privacy)
            assertThat(stillingSlettet?.status).isEqualTo("DELETED")
            assertThat(it.statusCode).isEqualTo(OK)
        }
    }

    @Test
    fun `DELETE mot stilling med kandidatlistefeil skal returnere status 500`() {
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
        val stilling =  Testdata.enDirektemeldtStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.stillingsId))

        direktemeldtStillingRepository.lagreDirektemeldtStilling(stilling)

        repository.opprett(stillingsinfo)
        val slettetStilling = stilling.copy(status = "DELETED")
        mockKandidatlisteSlettet()
        mockAzureObo(wiremockAzure)
        repository.hentForStilling(Stillingsid(stilling.stillingsId)) ?: fail("Setup")

        // når vi sletter stillingen
        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${slettetStilling.stillingsId}",
            HttpMethod.DELETE,
            HttpEntity(null, null),
            FrontendStilling::class.java
        ).also {
            val stillingIRespons = it.body!!
            assertThat(it.statusCode == OK)
            assertThat(stillingIRespons.status).isEqualTo("DELETED")
        }

        // så skal stillingens Stillingsinfo ikke slettes
        repository.hentForStilling(Stillingsid(stilling.stillingsId)) ?: fail("Det skal finnes en Stillingsinfo i db for ")

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

    @AfterEach
    fun after() {
        testRepository.slettAlt()
    }

}
