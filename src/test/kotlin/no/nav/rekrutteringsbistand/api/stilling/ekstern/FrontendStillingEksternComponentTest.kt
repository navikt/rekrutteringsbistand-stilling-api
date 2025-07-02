package no.nav.rekrutteringsbistand.api.stilling.ekstern

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.styrk
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.Kategori
import no.nav.rekrutteringsbistand.api.stilling.FrontendStilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class FrontendStillingEksternComponentTest {

    @Value("\${vis-stilling.azp-name}")
    private val uriTilVisStilling: String = "uriTilVisStilling"

    companion object {
        @JvmStatic
        @RegisterExtension
        val wiremockAzure: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9954))
            .build()

        @JvmStatic
        @RegisterExtension
        val wiremock: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9934))
            .build()
    }

    @Autowired
    lateinit var mockLogin: MockLogin

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @MockitoBean
    lateinit var stillingssokProxyClient: StillingssokProxyClient

    val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @AfterEach
    fun tearDown() {
        testRepository.slettAlt()
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        val stilling = enStilling
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)
        val token = mockLogin.hentAzureAdMaskinTilMaskinToken(uriTilVisStilling)

        whenever(stillingssokProxyClient.hentStilling(stilling.uuid, true)).thenReturn(stilling)

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
            assertThat(it.body).isEqualTo(forventetStillingForPersonbruker(stilling, null))
        }
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling med STYRK-navn som tittel for interne stillinger med kategori STILLING`() {
        testTittelPåStillingFor(
            stillingskategori = Stillingskategori.STILLING,
            source = "DIR",
            arbeidsplassentittel = "Ikke et STYRK-kodenavn",
            categoryList = listOf(styrk),
            forventetTittel = styrk.name!!,
        )
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling med orginaltittel som tittel for eksterne stillinger med kategori STILLING`() {
        testTittelPåStillingFor(
            stillingskategori = Stillingskategori.STILLING,
            source = "AMEDIA",
            arbeidsplassentittel = "Orginaltittel fra arbeidsplassen",
            categoryList = listOf(styrk),
            forventetTittel ="Orginaltittel fra arbeidsplassen"
        )
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling med invitasjon til jobbmesse som tittel for stillinger med kategori JOBBMESSE`() {
        testTittelPåStillingFor(
            stillingskategori = Stillingskategori.JOBBMESSE,
            source = "DIR",
            arbeidsplassentittel = "Orginaltittel fra arbeidsplassen",
            categoryList = listOf(styrk),
            forventetTittel ="Invitasjon til jobbmesse"
        )
    }

    private fun testTittelPåStillingFor(
        stillingskategori: Stillingskategori,
        source: String,
        arbeidsplassentittel: String,
        categoryList: List<Kategori>,
        forventetTittel: String,
    ) {
        val stilling = enStilling.copy(title = arbeidsplassentittel, categoryList = categoryList, source = source)
        val stillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = Stillingsid(stilling.uuid),
            eier = null,
            stillingskategori = stillingskategori,
        )
        repository.opprett(stillingsinfo)
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)
        val token = mockLogin.hentAzureAdMaskinTilMaskinToken(uriTilVisStilling)

        whenever(stillingssokProxyClient.hentStilling(stilling.uuid, true)).thenReturn(stilling)

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

    private fun forventetStillingForPersonbruker(stilling: FrontendStilling, stillingskategori: Stillingskategori?): StillingForPersonbruker {
        require(stilling.employer == null) { "Testkode er ikke tilpasset at employer er noe annet enn null" }
        return StillingForPersonbruker(
            id = stilling.id,
            annonsenr = stilling.annonsenr,
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
}
