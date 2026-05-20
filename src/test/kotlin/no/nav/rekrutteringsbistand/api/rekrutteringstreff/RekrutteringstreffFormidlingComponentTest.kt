package no.nav.rekrutteringsbistand.api.rekrutteringstreff

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.OpprettRekrutteringstreffFormidling
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.OpprettRekrutteringstreffFormidlingRespons
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.RekrutteringstreffStilling
import no.nav.rekrutteringsbistand.api.stilling.Arbeidsgiver
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.stilling.Geografi
import no.nav.rekrutteringsbistand.api.stilling.Kategori
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RekrutteringstreffFormidlingComponentTest {

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

    @MockitoBean
    lateinit var geografiService: GeografiService

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var mockLogin: MockLogin

    @Autowired
    lateinit var stillingsinfoRepository: StillingsinfoRepository

    @Autowired
    lateinit var direktemeldtStillingRepository: DirektemeldtStillingRepository

    @Autowired
    lateinit var testRepository: TestRepository

    private val restTemplate = TestRestTemplate()

    @BeforeEach
    fun setUp() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
        whenever(geografiService.populerGeografi(any())).thenAnswer { it.arguments[0] }
        whenever(geografiService.populerLocationList(any())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun cleanUp() {
        testRepository.slettAlt()
    }

    @Test
    fun `POST opprett formidling for rekrutteringstreff returnerer 200 med kandidatlisteId og stillingsId`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val request = lagOpprettFormidlingRequest()

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.kandidatlisteId).isNotNull
        assertThat(response.body!!.stillingsId).isNotNull
    }

    @Test
    fun `POST opprett formidling skal lagre stilling med riktig kategori REKRUTTERINGSTREFF_FORMIDLING`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val rekrutteringstreffId = UUID.randomUUID()
        val request = lagOpprettFormidlingRequest(rekrutteringstreffId = rekrutteringstreffId)

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val stillingsId = response.body!!.stillingsId

        val stillingsinfo = stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId))
        assertThat(stillingsinfo).isNotNull
        assertThat(stillingsinfo!!.stillingskategori).isEqualTo(Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING)
    }

    @Test
    fun `POST opprett formidling skal lagre rekrutteringstreffId i stillingsinfo`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val rekrutteringstreffId = UUID.randomUUID()
        val request = lagOpprettFormidlingRequest(rekrutteringstreffId = rekrutteringstreffId)

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val stillingsId = response.body!!.stillingsId

        val stillingsinfo = stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId))
        assertThat(stillingsinfo).isNotNull
        assertThat(stillingsinfo!!.rekrutteringstreffId).isEqualTo(rekrutteringstreffId)
    }

    @Test
    fun `POST opprett formidling skal lagre stilling med status STOPPED`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val request = lagOpprettFormidlingRequest()

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        val stillingsId = response.body!!.stillingsId
        val stilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)

        assertThat(stilling).isNotNull
        assertThat(stilling!!.status).isEqualTo("STOPPED")
    }

    @Test
    fun `POST opprett formidling skal lagre stilling med privacy INTERNAL_NOT_SHOWN`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val request = lagOpprettFormidlingRequest()

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        val stillingsId = response.body!!.stillingsId
        val stilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)

        assertThat(stilling).isNotNull
        assertThat(stilling!!.innhold.privacy).isEqualTo("INTERNAL_NOT_SHOWN")
        assertThat(stilling.innhold.source).isEqualTo("DIR")
        assertThat(stilling.innhold.medium).isEqualTo("DIR")
    }

    @Test
    fun `POST opprett formidling skal bruke JANZZ-kategori som tittel`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val janzzNavn = "Sykepleier"
        val request = lagOpprettFormidlingRequest(
            categoryList = listOf(
                Kategori(id = null, code = "1234", categoryType = "JANZZ", name = janzzNavn, description = null, parentId = null)
            )
        )

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        val stillingsId = response.body!!.stillingsId
        val stilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)

        assertThat(stilling!!.innhold.title).isEqualTo(janzzNavn)
    }

    @Test
    fun `POST opprett formidling skal bruke fallback-tittel når det ikke finnes JANZZ-kategori`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val request = lagOpprettFormidlingRequest(
            categoryList = listOf(
                Kategori(id = null, code = "3112.12", categoryType = "STYRK08NAV", name = "Byggeleder", description = null, parentId = null)
            )
        )

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        val stillingsId = response.body!!.stillingsId
        val stilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)

        assertThat(stilling!!.innhold.title).isEqualTo("Rekrutteringstreff-formidling")
    }

    @Test
    fun `POST opprett formidling skal filtrere bort tomme properties`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()

        val request = lagOpprettFormidlingRequest(
            properties = mapOf(
                "sector" to "Offentlig",
                "tomVerdi" to "",
                "tomListe" to "[]",
                "gyldigVerdi" to "noe"
            )
        )

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            OpprettRekrutteringstreffFormidlingRespons::class.java
        )

        val stillingsId = response.body!!.stillingsId
        val stilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)

        assertThat(stilling!!.innhold.properties).containsKey("sector")
        assertThat(stilling.innhold.properties).containsKey("gyldigVerdi")
        assertThat(stilling.innhold.properties).doesNotContainKey("tomVerdi")
        assertThat(stilling.innhold.properties).doesNotContainKey("tomListe")
    }

    @Test
    fun `POST opprett formidling skal returnere 500 når kandidatliste-kallet feiler, og ikke lagre stilling og stillingsinfo`() {
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdateringFeiler()

        val request = lagOpprettFormidlingRequest()

        val response = restTemplate.postForEntity(
            "$localBaseUrl/rekrutteringstreff/formidling",
            request,
            String::class.java
        )
        val antallStillinger = testRepository.hentAntallDirektemeldtStilling()
        val antallStilllingsinfo = testRepository.hentAntallStillingsinfo()

        assertThat(antallStillinger).isEqualTo(0)
        assertThat(antallStilllingsinfo).isEqualTo(0)
        assertThat(response.statusCode.is5xxServerError).isTrue()
    }

    private fun lagOpprettFormidlingRequest(
        rekrutteringstreffId: UUID = UUID.randomUUID(),
        eierNavKontorEnhetId: String = "0318",
        categoryList: List<Kategori> = listOf(
            Kategori(id = null, code = "1234", categoryType = "JANZZ", name = "Sykepleier", description = null, parentId = null)
        ),
        properties: Map<String, String> = emptyMap()
    ): OpprettRekrutteringstreffFormidling {
        return OpprettRekrutteringstreffFormidling(
            eierNavKontorEnhetId = eierNavKontorEnhetId,
            rekrutteringstreffId = rekrutteringstreffId,
            stilling = RekrutteringstreffStilling(
                employer = Arbeidsgiver(
                    id = null,
                    uuid = null,
                    created = null,
                    createdBy = null,
                    updated = null,
                    updatedBy = null,
                    mediaList = emptyList(),
                    contactList = emptyList(),
                    location = Geografi(
                        address = "Storgata 1",
                        postalCode = "0182",
                        county = null,
                        country = "NORGE",
                        municipal = null,
                        municipalCode = null,
                        city = "OSLO",
                        latitude = null,
                        longitude = null
                    ),
                    locationList = emptyList(),
                    properties = emptyMap(),
                    name = "Test Bedrift AS",
                    orgnr = "123456789",
                    status = null,
                    parentOrgnr = "987654321",
                    publicName = "Test Bedrift",
                    deactivated = null,
                    orgform = "AS",
                    employees = 10
                ),
                locationList = listOf(
                    Geografi(
                        address = null,
                        postalCode = "0182",
                        county = null,
                        country = "NORGE",
                        municipal = null,
                        municipalCode = null,
                        city = "OSLO",
                        latitude = null,
                        longitude = null
                    )
                ),
                categoryList = categoryList,
                properties = properties
            )
        )
    }

    private fun mockKandidatlisteOppdatering() {
        wiremockKandidatliste.stubFor(
            put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/kandidatliste"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withHeader(CONNECTION, "close")
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody("""{"kandidatlisteId": "${UUID.randomUUID()}"}""")
                )
        )
    }

    private fun mockKandidatlisteOppdateringFeiler() {
        wiremockKandidatliste.stubFor(
            put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/kandidatliste"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader(CONNECTION, "close")
                )
        )
    }
}
