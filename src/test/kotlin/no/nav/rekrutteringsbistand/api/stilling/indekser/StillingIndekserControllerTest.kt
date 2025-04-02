package no.nav.rekrutteringsbistand.api.stilling.indekser

import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
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
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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
    fun `Stilling blir publisert på rapid når endepunkt blir kalt`() {
        val direktemeldtStilling = enDirektemeldtStilling
        val direktemeldtStilling2 = enDirektemeldtStilling.copy(
            stillingsid = UUID.randomUUID(),
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
            .uri(URI("$baseUrl/stillinger/reindekser"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        Thread.sleep(3000) // Må vente for at tråden skal ha kjørt all koden
        verify(rapidApp, times(2)).publish(idCaptor.capture(), any())

        val capturedId = idCaptor.firstValue
        assertEquals(enDirektemeldtStilling.stillingsid, capturedId.verdi)

        val capturedId2 = idCaptor.secondValue
        assertEquals(direktemeldtStilling2.stillingsid, capturedId2.verdi)

        assertEquals(200, response.statusCode())
    }
}
