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
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stilling.Stilling
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
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = arrayOf("external.pam-ad-api.url=http://localhost:9935")
)
class MineStillingerTest {

    @get:Rule
    val wiremockPamAdApi = WireMockRule(
        WireMockConfiguration.options().port(9935).notifier(Slf4jNotifier(true))
            .extensions(ResponseTemplateTransformer(true))
    )

    @Autowired
    lateinit var mockLogin: MockLogin

    @Autowired
    lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var repository: MineStillingerRepository

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


    @Before
    fun before() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate, navIdent)
        jdbcTemplate.jdbcOperations.execute("truncate table min_stilling")
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
        val stilingerFraDb = repository.hent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(stillingFraRespons.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(stillingFraRespons.id)
        assertThat(stillingFraDb.status).isEqualTo(stillingFraRespons.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(stillingFraRespons.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.updated)
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(stillingFraRespons.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(stillingFraRespons.title)
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

        val stilingerFraDb = repository.hent(navIdent)
        assertThat(stilingerFraDb.size).isEqualTo(1)
        val stillingFraDb = stilingerFraDb.first()
        assertThat(stillingFraDb.stillingsId.asString()).isEqualTo(oppdatertPamAdStilling.uuid)
        assertThat(stillingFraDb.annonsenr).isEqualTo(oppdatertPamAdStilling.id)
        assertThat(stillingFraDb.status).isEqualTo(oppdatertPamAdStilling.status)
        assertThat(stillingFraDb.arbeidsgiverNavn).isEqualTo(oppdatertPamAdStilling.businessName)
        assertThat(stillingFraDb.sistEndret.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertPamAdStilling.updated) // TODO: Blir vel ikke riktig?
        assertThat(stillingFraDb.utløpsdato.toLocalDateTime()).isEqualToIgnoringNanos(oppdatertPamAdStilling.expires)
        assertThat(stillingFraDb.tittel).isEqualTo(oppdatertPamAdStilling.title)
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


    /*
    Test cases:
    - Når veileder oppdaterer en direktemelding stilling, så skal vi lagre de oppdaterte verdiene
    - Når veileder lager kandidatliste for en ekstern stilling, så skal vi lagre det vi trenger for å vise den fram i Mine stillinger
    - Når stilling-api konsumerer en melding som gjelder en ekstern stilling vi allerede har lagret data på, så skal vi oppdatere verdiene
    - Når veileder sletter en direktemeldt stilling, så skal vi slette den fra tabellen
    - Når stilling-api konsumerer en melding om en slettet stilling, så skal vi slette stillingen
    - Når veileder overtar en direktemeldt stilling, skal vi oppdatere eier i tabellen
    - Når veileder overtar en ekstern stilling, skal vi oppdatere eier i tabellen
    - Når veileder oppdaterer en direktemeldt stilling so en ikke eier så skal vi IKKE endre eier på stillingen
     */
}
