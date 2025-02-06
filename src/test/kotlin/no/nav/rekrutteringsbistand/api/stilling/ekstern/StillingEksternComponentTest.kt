package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStilling
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.stilling.Kategori
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.junit4.SpringRunner
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
internal class StillingEksternComponentTest {

    @Value("\${vis-stilling.azp-name}")
    private val uriTilVisStilling: String = "uriTilVisStilling"

    @get:Rule
    val wiremock = WireMockRule(9934)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(8766)

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @Autowired
    lateinit var mockLogin: MockLogin

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var direktemeldtStillingRepository: DirektemeldtStillingRepository

    @Autowired
    lateinit var testRepository: TestRepository

    val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @After
    fun tearDown() {
        testRepository.slettAlt()
    }

    @Test
    fun test() {
        val stilling = enStilling
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        val stilling = enStilling
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)
        val token = mockLogin.hentAzureAdMaskinTilMaskinToken(uriTilVisStilling)

        val direktemeldtStilling = mockHentDirektemeldtStilling(stilling.uuid, stilling)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistand/ekstern/api/v1/stilling/${stilling.uuid}",
            HttpMethod.GET,
            HttpEntity(
                null, mapOf(
                    AUTHORIZATION to "Bearer $token"
                ).toMultiValueMap()
            ),
            StillingForPersonbruker::class.java
        ).also {
           // assertThat(it.body).isEqualTo(forventetStillingForPersonbruker(stilling, null))
        }
    }
//
//    @Test
//    fun `GET mot en stilling skal returnere en stilling med STYRK-navn som tittel for interne stillinger med kategori STILLING`() {
//        testTittelPåStillingFor(
//            stillingskategori = Stillingskategori.STILLING,
//            source = "DIR",
//            arbeidsplassentittel = "Ikke et STYRK-kodenavn",
//            categoryList = listOf(styrk),
//            forventetTittel = styrk.name!!,
//        )
//    }
//
//    @Test
//    fun `GET mot en stilling skal returnere en stilling med orginaltittel som tittel for eksterne stillinger med kategori STILLING`() {
//        testTittelPåStillingFor(
//            stillingskategori = Stillingskategori.STILLING,
//            source = "AMEDIA",
//            arbeidsplassentittel = "Orginaltittel fra arbeidsplassen",
//            categoryList = listOf(styrk),
//            forventetTittel ="Orginaltittel fra arbeidsplassen"
//        )
//    }
//
//    @Test
//    fun `GET mot en stilling skal returnere en stilling med invitasjon til jobbmesse som tittel for stillinger med kategori JOBBMESSE`() {
//        testTittelPåStillingFor(
//            stillingskategori = Stillingskategori.JOBBMESSE,
//            source = "DIR",
//            arbeidsplassentittel = "Orginaltittel fra arbeidsplassen",
//            categoryList = listOf(styrk),
//            forventetTittel ="Invitasjon til jobbmesse"
//        )
//    }

    private fun testTittelPåStillingFor(
        stillingskategori: Stillingskategori,
        source: String,
        arbeidsplassentittel: String,
        categoryList: List<Kategori>,
        forventetTittel: String,
    ) {
        val stilling = enStilling.copy(title = arbeidsplassentittel, categoryList = categoryList, source = source)
        val stillingsinfo = Stillingsinfo(
            stillingsid = Stillingsid(stilling.uuid),
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingskategori = stillingskategori,
            eier = null
        )
        repository.opprett(stillingsinfo)
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)
        val token = mockLogin.hentAzureAdMaskinTilMaskinToken(uriTilVisStilling)

        mockHentDirektemeldtStilling(stilling.uuid, stilling)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistand/ekstern/api/v1/stilling/${stilling.uuid}",
            HttpMethod.GET,
            HttpEntity(
                null, mapOf(
                    AUTHORIZATION to "Bearer $token"
                ).toMultiValueMap()
            ),
            StillingForPersonbruker::class.java
        ).also {
            assertThat(it.body).isEqualTo(forventetStillingForPersonbruker(stilling, stillingskategori).copy(title = forventetTittel))
        }
    }


    fun mockUtenAuthorization(urlPath: String, responseBody: Any) {
        wiremock.stubFor(
            get(urlPathMatching(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(
                    aResponse().withStatus(200)
                        .withHeader(
                            CONNECTION,
                            "close"
                        ) // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(objectMapper.writeValueAsString(responseBody))
                )
        )
    }

    private fun forventetStillingForPersonbruker(stilling: Stilling, stillingskategori: Stillingskategori?): StillingForPersonbruker {
        require(stilling.employer == null) { "Testkode er ikke tilpasset at employer er noe annet enn null" }
        return StillingForPersonbruker(
            id = null,
            updated = stilling.updated,
            title = stilling.title,
            medium = stilling.medium,
            employer = null,
            location = stilling.location,
            properties = stilling.properties,
            businessName = stilling.businessName,
            status = stilling.status,
            uuid = stilling.uuid,
            source = stilling.source,
            stillingskategori = stillingskategori,
        )
    }

    fun mockHentDirektemeldtStilling(stillingsId: String, stilling: Stilling): DirektemeldtStilling {
        val direktemeldtStilling = DirektemeldtStilling(
            stillingsId = UUID.fromString(stillingsId),
            innhold = stilling.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling.status,
            annonseId = stilling.id
        )
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)

        return direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)
    }
}
