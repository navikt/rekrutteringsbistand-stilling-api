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
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettetStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.HttpMethod.*
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.net.URI
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

    private lateinit var stillingConsumer: StillingConsumer

    @Before
    fun before() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate, navIdent)
        jdbcTemplate.jdbcOperations.execute("truncate table min_stilling")
        if (!this::stillingConsumer.isInitialized) {
            stillingConsumer = StillingConsumer(MineStillingerService(repository))
        }
    }

    @Test
    fun `Når veileder oppretter en direktemeldt stilling skal vi lagre den i db-tabellen`() {
        val rekrutteringsbistandStilling = Testdata.enOpprettRekrutteringsbistandstillingDto
        mockPamAdApi(POST, "/api/v1/ads", enOpprettetStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        val respons = restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            rekrutteringsbistandStilling,
            RekrutteringsbistandStilling::class.java
        )

        val stillingFraRespons = respons.stilling
        val stilingerFraDb = hentAlle()
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
    fun `Når veileder oppdaterer en direktemeldt stilling så skal vi lagre de oppdaterte verdiene`() {
        val pamAdStilling: Stilling = enOpprettetStilling
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

        val stilingerFraDb = hentAlle()
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
    fun `Når veileder kopierer en direktemeldt stilling skal vi lagre den nye i db-tabellen`() {
        val eksisterendeRekrutteringsbistandStilling = enRekrutteringsbistandStilling
        val eksisterendePamAdStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val eksisterendeMinStilling: MinStilling = MinStilling.fromStilling(eksisterendePamAdStilling, navIdent)
        repository.opprett(eksisterendeMinStilling)
        mockPamAdApi(GET, "/b2b/api/v1/ads/${eksisterendePamAdStilling.uuid}", eksisterendePamAdStilling)
        val nyKopiAvStilling = enOpprettetStilling.copy(eksisterendePamAdStilling.id + 1, uuid = UUID.randomUUID().toString())
        mockPamAdApi(POST, "/api/v1/ads", nyKopiAvStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        val respons = restTemplate.exchange(
            URI("$localBaseUrl/rekrutteringsbistandstilling/kopier/${eksisterendePamAdStilling.uuid}"),
            POST,
            HttpEntity(
                null,
                mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE
                ).toMultiValueMap()
            ),
            RekrutteringsbistandStilling::class.java
        )

        val stillingFraRespons = respons.body!!.stilling
        val stilingerFraDb = hentAlle()
        assertThat(stilingerFraDb.size).isEqualTo(2)
        val stillingFraDb = stilingerFraDb.first { it.stillingsId.asString() == stillingFraRespons!!.uuid}
        assertThat(stillingFraDb.annonsenr).isEqualTo(stillingFraRespons.id)
        assertThat(stillingFraDb.status).isEqualTo(stillingFraRespons.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(stillingFraRespons.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.updated)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(stillingFraRespons.title)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Når veileder sletter en direktemeldt stilling skal minStilling også slettes`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStilling
        val pamAdStilling = rekrutteringsbistandStilling.stilling
        val eksisterendeMinStilling: MinStilling = MinStilling.fromStilling(pamAdStilling, navIdent)
        repository.opprett(eksisterendeMinStilling)
        mockKandidatlisteSlettet()
        mockPamAdApi(DELETE, "/api/v1/ads/${pamAdStilling.uuid}", pamAdStilling)
        mockAzureObo(wiremockAzure)
        val annenPamAdStilling = rekrutteringsbistandStilling.stilling.copy(
            id = pamAdStilling.id + 1,
            uuid = UUID.randomUUID().toString())
        val minStillingSomIkkeSkalSlettes = MinStilling.fromStilling(annenPamAdStilling, navIdent)
        repository.opprett(minStillingSomIkkeSkalSlettes)

        restTemplate.delete("$localBaseUrl/rekrutteringsbistandstilling/${pamAdStilling.uuid}")

        val mineStillinger = hentAlle()
        assertThat(mineStillinger.size).isOne
        assertThat(mineStillinger.first().stillingsId).isEqualTo(minStillingSomIkkeSkalSlettes.stillingsId)
    }

    @Test
    fun `Når veileder oppretter kandidatliste for ekstern stilling skal vi opprette stillingen i db-tabellen`() {
        mockAzureObo(wiremockAzure)
        val stillingsinfoDto = Testdata.enStillingsinfoInboundDto.copy(eierNavident = navIdent)
        val pamAdStilling = enOpprettetStilling.copy(uuid = stillingsinfoDto.stillingsid)
        mockPamAdApi(GET, "/b2b/api/v1/ads/${pamAdStilling.uuid}", pamAdStilling)
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${pamAdStilling.uuid}", pamAdStilling)
        mockKandidatlisteOppdatering()

        restTemplate.put(
            "$localBaseUrl/stillingsinfo",
            stillingsinfoDto,
            StillingsinfoDto::class.java
        )

        val stilingerFraDb = hentAlle()
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
        val pamAdStilling: Stilling = enOpprettetStilling
        val eksisterendeMinStilling: MinStilling = MinStilling.fromStilling(pamAdStilling, navIdent)
        repository.opprett(eksisterendeMinStilling)

        val oppdatertTittel = pamAdStilling.title + "dummy"
        val oppdatertStatus = AdStatus.REJECTED
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

        stillingConsumer.konsumerMelding(melding)

        val stilingerFraDb = hentAlle()
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(pamAdStilling.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(pamAdStilling.id)
        assertThat(stillingFraDb.status).isEqualTo(oppdatertStatus.toString())
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(oppdatertArbeidsgiverNavn)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertSistEndret)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertUtløpsdato)
        assertThat(stillingFraDb.tittel).isEqualTo(oppdatertTittel)
        assertThat(stillingFraDb.eierNavIdent).isEqualTo(navIdent)
    }

    @Test
    fun `Skal ignorere meldinger for stillinger som ikke er lagret`() {
        val melding = stillingsmelding()
        stillingConsumer.konsumerMelding(melding)
        assertThat(hentAlle()).isEmpty()
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
                ).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun mockKandidatlisteSlettet() {
        wiremockKandidatliste.stubFor(
            WireMock.delete(WireMock.urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste"))
                .withHeader(
                    HttpHeaders.CONTENT_TYPE,
                    WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)
            ).withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON_VALUE)).willReturn(
                WireMock.aResponse().withStatus(HttpStatus.NO_CONTENT.value()).withHeader(
                    HttpHeaders.CONNECTION, "close"
                ).withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            )
        )
    }

    private fun stillingsmelding(
        stillingsId: String = UUID.randomUUID().toString(),
        annonsenr: Long = 1,
        tittel: String = "dummyTittel",
        status: AdStatus = AdStatus.INACTIVE,
        arbeidsgiverNavn: String = "dummyArbeidsgiversNavn",
        sistEndret: LocalDateTime = LocalDateTime.now(),
        utløpsdato: LocalDateTime = LocalDateTime.now(),
        kilde: String = "IMPORTAPI"
    ): ConsumerRecord<String, Ad> {
        val ad = Ad().apply {
            this.setUuid(stillingsId)
            this.setAdnr(annonsenr.toString())
            this.setTitle(tittel)
            this.setStatus(status)
            this.setBusinessName(arbeidsgiverNavn)
            this.setUpdated(sistEndret.toString())
            this.setExpires(utløpsdato.toString())
            this.setSource(kilde)
        }
        return ConsumerRecord<String, Ad>("dummy", 0, 0L, stillingsId, ad)
    }

    private fun hentAlle(): List<MinStilling> {
        val sql = "select * from min_stilling"

        return jdbcTemplate.query(sql) { rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }
    }

    /*
    Test cases:
    - Når stilling-api konsumerer en melding om en slettet stilling, så skal vi slette stillingen
    - Når veileder overtar en direktemeldt stilling, skal vi oppdatere eier i tabellen
    - Når veileder overtar en ekstern stilling, skal vi oppdatere eier i tabellen
    - Når veileder oppdaterer en direktemeldt stilling so en ikke eier så skal vi IKKE endre eier på stillingen
     */
}
