package no.nav.rekrutteringsbistand.api.minestillinger

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon.Companion.registrerMineStillingerLytter
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = arrayOf("external.pam-ad-api.url=http://localhost:9934")
)
class MineStillingerTest {

    @get:Rule
    val wiremockPamAdApi = WireMockRule(
        WireMockConfiguration.options().port(9934).notifier(Slf4jNotifier(true))
            .extensions(ResponseTemplateTransformer(true))
    )

    @Autowired
    private lateinit var mockLogin: MockLogin

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var repository: MineStillingerRepository

    @LocalServerPort
    private var port = 0

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(8766)

    private val restTemplate = TestRestTemplate()

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val objectMapper: ObjectMapper =
        ObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private val navIdent = "dummy"

    private lateinit var testRapid: TestRapid

    @Before
    fun before() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate, navIdent)
        jdbcTemplate.jdbcOperations.execute("truncate table min_stilling")
        if (!this::testRapid.isInitialized) testRapid =
            TestRapid().registrerMineStillingerLytter(MineStillingerService(repository))
        testRapid.reset()
    }

    @Test
    fun `Når veileder oppretter en direktemeldt stilling skal vi lagre den i db-tabellen`() {
        val rekrutteringsbistandStilling = Testdata.enOpprettRekrutteringsbistandstillingDto
        mockPamAdApi(HttpMethod.POST, "/api/v1/ads", Testdata.enOpprettetStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        val respons = restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            rekrutteringsbistandStilling,
            RekrutteringsbistandStilling::class.java
        )

        val stillingFraRespons = respons.stilling
        val stilingerFraDb = repository.hentForNavIdent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(stillingFraRespons.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(stillingFraRespons.id)
        assertThat(stillingFraDb.status).isEqualTo(stillingFraRespons.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(stillingFraRespons.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.updated)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(stillingFraRespons.title)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Når veileder oppdaterer en direktemelding stilling så skal vi lagre de oppdaterte verdiene`() {
        val pamAdStilling: Stilling = Testdata.enOpprettetStilling
        val eksisterendeMinStilling: MinStilling = MinStilling.fromStilling(pamAdStilling, navIdent)
        repository.opprett(eksisterendeMinStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)
        val oppdatertPamAdStilling = pamAdStilling.copy(
            title = pamAdStilling.title + "dummy",
            expires = pamAdStilling.expires!!.plusMonths(1),
            businessName = pamAdStilling.businessName + "dummy",
            status = pamAdStilling.status + "dummy",
        )
        val oppdatertStillingDto = OppdaterRekrutteringsbistandStillingDto(
            stilling = oppdatertPamAdStilling,
            stillingsinfoid = "dummy",
            notat = "dummy"
        )
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${pamAdStilling.uuid}", oppdatertPamAdStilling)

        restTemplate.put(
            "$localBaseUrl/rekrutteringsbistandstilling",
            oppdatertStillingDto,
            RekrutteringsbistandStilling::class.java
        )

        val stilingerFraDb = repository.hentForNavIdent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(oppdatertPamAdStilling.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(oppdatertPamAdStilling.id)
        assertThat(stillingFraDb.status).isEqualTo(oppdatertPamAdStilling.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(oppdatertPamAdStilling.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertPamAdStilling.updated)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertPamAdStilling.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(oppdatertPamAdStilling.title)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Når veileder oppretter kandidatliste for ekstern stilling skal vi opprette stillingen i db-tabellen`() {
        mockAzureObo(wiremockAzure)
        val stillingsinfoDto = Testdata.enStillingsinfoInboundDto.copy(eierNavident = navIdent)
        val pamAdStilling = Testdata.enOpprettetStilling.copy(uuid = stillingsinfoDto.stillingsid)
        mockPamAdApi(HttpMethod.GET, "/b2b/api/v1/ads/${pamAdStilling.uuid}", pamAdStilling)
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${pamAdStilling.uuid}", pamAdStilling)
        mockKandidatlisteOppdatering()

        restTemplate.put(
            "$localBaseUrl/stillingsinfo",
            stillingsinfoDto,
            StillingsinfoDto::class.java
        )

        val stilingerFraDb = repository.hentForNavIdent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(pamAdStilling.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(pamAdStilling.id)
        assertThat(stillingFraDb.status).isEqualTo(pamAdStilling.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(pamAdStilling.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(pamAdStilling.updated)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(pamAdStilling.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(pamAdStilling.title)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Når en melding for ekstern stilling som vi har lagret konsumeres skal vi lagre oppdaterte verdier`() {
        val pamAdStilling: Stilling = Testdata.enOpprettetStilling
        val eksisterendeMinStilling: MinStilling = MinStilling.fromStilling(pamAdStilling, navIdent)
        repository.opprett(eksisterendeMinStilling)

        val oppdatertTittel = pamAdStilling.title + "dummy"
        val oppdatertStatus = pamAdStilling.status + "dummy"
        val oppdatertArbeidsgiverNavn = pamAdStilling.businessName + "dummy"
        val oppdatertSistEndret = pamAdStilling.updated.plusDays(1)
        val oppdatertUtløpsdato = pamAdStilling.expires!!.plusDays(1)
        val melding = stillingsmelding(
            stillingsId = pamAdStilling.uuid,
            annonsenr = pamAdStilling.id,
            tittel = oppdatertTittel,
            status = oppdatertStatus,
            arbeidsgiverNavn = oppdatertArbeidsgiverNavn,
            sistEndret = oppdatertSistEndret,
            utløpsdato = oppdatertUtløpsdato
        )
        testRapid.sendTestMessage(melding)

        val stilingerFraDb = repository.hentForNavIdent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(pamAdStilling.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(pamAdStilling.id)
        assertThat(stillingFraDb.status).isEqualTo(oppdatertStatus)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(oppdatertArbeidsgiverNavn)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertSistEndret)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertUtløpsdato)
        assertThat(stillingFraDb.tittel).isEqualTo(oppdatertTittel)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Skal ignorere meldinger for stillinger som ikke er lagret`() {
        val melding = stillingsmelding()
        testRapid.sendTestMessage(melding)
        assertThat(hentAlleMineStillinger()).isEmpty()
    }

    private fun mockPamAdApi(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            WireMock.request(method.name(), WireMock.urlPathMatching(urlPath))
                .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE))
                .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).withHeader(
                    HttpHeaders.AUTHORIZATION,
                    WireMock.matching("Bearer .*")
                )
                .willReturn(
                    WireMock.aResponse().withStatus(200).withHeader(
                        HttpHeaders.CONNECTION, "close"
                    )
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )
    }

    private fun mockKandidatlisteOppdatering(metodeFunksjon: (UrlPattern) -> MappingBuilder = WireMock::put) {
        wiremockKandidatliste.stubFor(
            metodeFunksjon(WireMock.urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/kandidatliste")).withHeader(
                HttpHeaders.CONTENT_TYPE,
                WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)
            ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).willReturn(
                WireMock.aResponse().withStatus(HttpStatus.NO_CONTENT.value()).withHeader(
                    HttpHeaders.CONNECTION, "close"
                ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun stillingsmelding(
        stillingsId: String = UUID.randomUUID().toString(),
        annonsenr: Long = 1,
        tittel: String = "dummyTittel",
        status: String = "dummyStatus",
        arbeidsgiverNavn: String = "dummyArbeidsgiversNavn",
        sistEndret: LocalDateTime = LocalDateTime.now(),
        utløpsdato: LocalDateTime = LocalDateTime.now(),
        kilde: String = "IMPORTAPI"
    ) = """
        {
          "uuid": "$stillingsId",
          "adnr": "$annonsenr",
          "title": "$tittel",
          "status": "$status",
          "privacy": "SHOW_ALL",
          "administration": "{\"status\": \"DONE\", \"remarks\": [], \"comments\": \"Auto approved - 2023-05-15T17:04:15.677443528\\n\", \"reportee\": \"AUTO\", \"navIdent\": \"\"}",
          "published": "2023-05-08T00:00:00",
          "expires": "$utløpsdato",
          "created": "2023-05-15T17:04:15.745674",
          "updated": "$sistEndret",
          "employer": "",
          "categories": "[]",
          "source": "$kilde",
          "medium": "talentech",
          "reference": "9f83702add0f494ba544eb88751b7a42",
          "publishedByAdmin": "2023-05-15T17:04:15.677549",
          "businessName": "$arbeidsgiverNavn",
          "locations": "[{\"address\": null, \"postalCode\": null, \"county\": null, \"municipal\": null, \"city\": null, \"country\": \"NORGE\", \"latitude\": null, \"longitude\": null, \"municipal_code\": null, \"county_code\": null}]",
          "properties": "[{\"key\": \"applicationdue\", \"value\": \"2023-05-21T00:00:00\"}, {\"key\": \"_providerid\", \"value\": \"15002\"}, {\"key\": \"jobtitle\", \"value\": \"test job\"}, {\"key\": \"positioncount\", \"value\": \"1\"}, {\"key\": \"_versionid\", \"value\": \"16700\"}, {\"key\": \"applicationurl\", \"value\": \"https://dummy.talentech.io/?utm_medium=talentech_publishing&utm_source=nav\"}, {\"key\": \"_approvedby\", \"value\": \"AUTO\"}, {\"key\": \"classification_esco_code\", \"value\": \"http://data.europa.eu/esco/occupation/6d03eaef-1ce5-4a1e-bb41-0908e1af1b65\"}, {\"key\": \"arbeidsplassenoccupation\", \"value\": \"Uoppgitt/ ikke identifiserbare/Ikke identifiserbare\"}, {\"key\": \"_score\", \"value\": \"[{\\\"name\\\":\\\"location\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"employer\\\",\\\"value\\\":-50},{\\\"name\\\":\\\"sector\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"engagementtype\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"extent\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"jobarrangement\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"jobpercentage\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"keywords\\\",\\\"value\\\":-10},{\\\"name\\\":\\\"employerdescription\\\",\\\"value\\\":-10}]\"}, {\"key\": \"_scoretotal\", \"value\": \"-130\"}, {\"key\": \"adtext\", \"value\": \"<h3>i18n mock translation</h3><br />\\n <br />\\n<h3>i18n mock translation:</h3>i18n mock translation: Unit_Test_Teant_21b2177e-587f-4d11-9500-45ad17cacfa1<br />\\ni18n mock translation: 29.05.2023<br />\\n<br />\\n\"}]",
          "contacts": "[]"
        }
    """.trimIndent()

    private fun hentAlleMineStillinger(): List<MinStilling> {
        val sql = "select * from min_stilling"

        return jdbcTemplate.query(sql) { rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }
    }

    /*
    Test cases:
    - Når veileder sletter en direktemeldt stilling, så skal vi slette den fra tabellen
    - Når stilling-api konsumerer en melding om en slettet stilling, så skal vi slette stillingen
    - Når veileder overtar en direktemeldt stilling, skal vi oppdatere eier i tabellen
    - Når veileder overtar en ekstern stilling, skal vi oppdatere eier i tabellen
    - Når veileder oppdaterer en direktemeldt stilling so en ikke eier så skal vi IKKE endre eier på stillingen
     */
}
