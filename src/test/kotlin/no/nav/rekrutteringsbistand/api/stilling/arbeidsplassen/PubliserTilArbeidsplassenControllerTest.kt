package no.nav.rekrutteringsbistand.api.stilling.arbeidsplassen

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PubliserTilArbeidsplassenControllerTest {

    @LocalServerPort
    var port = 0

    private lateinit var baseUrl: String

    @MockitoBean
    lateinit var stillingService: StillingService

    @MockitoBean
    lateinit var rapidApp: RapidApplikasjon

    @MockitoBean
    lateinit var stillingsinfoService: StillingsinfoService

    val idCaptor = argumentCaptor<Stillingsid>()

    @BeforeEach
    fun setup() {
        baseUrl = "http://localhost:$port"
    }

    @Test
    fun `Stilling blir publisert på rapid når endepunkt blir kalt`() {
        val enDirektemeldtStilling = enDirektemeldtStilling

        whenever(stillingService.hentDirektemeldtStilling(any())).thenReturn(enDirektemeldtStilling)

        val uuid = enDirektemeldtStilling.stillingsId.toString()
        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/arbeidsplassen/publiser/$uuid"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        verify(rapidApp).publish(idCaptor.capture(), any())

        val capturedId = idCaptor.firstValue
        assertEquals(enDirektemeldtStilling.stillingsId, capturedId.verdi)
        assertEquals(200, response.statusCode() )
    }

    @Test
    fun `stillingsinfo sendes som JSON null når stilling mangler stillingsinfo`() {
        whenever(stillingService.hentDirektemeldtStilling(any())).thenReturn(enDirektemeldtStilling)
        whenever(stillingsinfoService.hentStillingsinfo(any())).thenReturn(null)

        val request = HttpRequest.newBuilder()
            .uri(URI("$baseUrl/arbeidsplassen/publiser/${enDirektemeldtStilling.stillingsId}"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

        val response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString())

        val packetCaptor = argumentCaptor<JsonMessage>()
        verify(rapidApp).publish(any(), packetCaptor.capture())
        val melding = jacksonObjectMapper().readTree(packetCaptor.firstValue.toJson())

        assertEquals(200, response.statusCode())
        assertTrue(melding["stillingsinfo"].isNull, "stillingsinfo skal være JSON null, var: ${melding["stillingsinfo"]}")
    }
}
