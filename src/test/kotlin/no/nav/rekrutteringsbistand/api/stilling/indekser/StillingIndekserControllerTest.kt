package no.nav.rekrutteringsbistand.api.stilling.indekser

import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class StillingIndekserControllerTest {

    @LocalServerPort
    var port = 0

    private lateinit var baseUrl: String

    @MockitoBean
    lateinit var stillingService: StillingService

    @MockitoBean
    lateinit var stillingsinfoService: StillingsinfoService

    @MockitoBean
    lateinit var rapidApp: RapidApplikasjon

    @Autowired
    lateinit var mockLogin: MockLogin

    val idCaptor = argumentCaptor<Stillingsid>()

    @BeforeEach
    fun setup() {
        baseUrl = "http://localhost:$port"
    }

    @Test
    fun `Alle stillinger i databasen blir publisert på rapid når endepunkt blir kalt`() {
        val direktemeldtStilling = enDirektemeldtStilling
        val direktemeldtStilling2 = enDirektemeldtStilling.copy(
            stillingsId = UUID.randomUUID(),
            innhold = enDirektemeldtStilling.innhold.copy(title = "Dette er en stilling")
        )

        whenever(stillingService.hentAlleDirektemeldteStillinger()).thenReturn(
            listOf(
                direktemeldtStilling,
                direktemeldtStilling2
            )
        )

        whenever(stillingsinfoService.hentForStilling(any())).thenReturn(null)

        val token = mockLogin.hentAzureAdMaskinTilMaskinToken("local:toi:toi-stilling-indekser")

        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/reindekser/stillinger"))
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        await().atMost(3, TimeUnit.SECONDS).untilAsserted {
            verify(rapidApp, times(2)).publish(idCaptor.capture(), any())
        }

        val capturedId = idCaptor.firstValue
        assertEquals(enDirektemeldtStilling.stillingsId, capturedId.verdi)

        val capturedId2 = idCaptor.secondValue
        assertEquals(direktemeldtStilling2.stillingsId, capturedId2.verdi)

        assertEquals(200, response.statusCode())
    }

    @Test
    fun `Innsendt stilling blir publisert på rapid når endepunkt blir kalt`() {
        whenever(stillingService.hentDirektemeldtStilling(any())).thenReturn(enDirektemeldtStilling)

        val uuid = enDirektemeldtStilling.stillingsId.toString()
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/reindekser/stilling/$uuid"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        verify(rapidApp).publish(idCaptor.capture(), any())

        val capturedId = idCaptor.firstValue
        assertEquals(enDirektemeldtStilling.stillingsId, capturedId.verdi)
        assertEquals(200, response.statusCode() )
    }
}
