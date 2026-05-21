package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.server.ResponseStatusException
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class StillingServiceTest {
    @Mock
    lateinit var stillingsinfoService: StillingsinfoService
    @Mock
    lateinit var tokenUtils: TokenUtils
    @Mock
    lateinit var kandidatlisteKlient: KandidatlisteKlient
    @Mock
    lateinit var direktemeldtStillingService: DirektemeldtStillingService
    @Mock
    lateinit var stillingssokProxyClient: StillingssokProxyClient
    @Mock
    lateinit var geografiService: GeografiService
    @Mock
    lateinit var stillingOutboxService: StillingOutboxService


    lateinit var stillingService: StillingService

    @BeforeEach
    fun setUp() {
        stillingService = StillingService(
            stillingsinfoService,
            tokenUtils,
            kandidatlisteKlient,
            direktemeldtStillingService,
            stillingssokProxyClient,
            geografiService,
            stillingOutboxService,
        )
    }

    @Test
    fun `Jobbmesse skal ikke kunne publiseres til arbeidsplassen`() {
        val stillingsid = Stillingsid("123e4567-e89b-12d3-a456-426614174000")
        val eier = Eier(navident = "Z123456", navn = "Ola Nordmann", navKontorEnhetId = "1234")
        val stillingsinfo = Stillingsinfo(
            stillingsid = stillingsid,
            stillingsinfoid = Stillingsinfoid("123e4567-e89b-12d3-a456-426614174001"),
            eier = eier,

            stillingskategori = Stillingskategori.JOBBMESSE,

        )
        val direktemeldtStilling = enDirektemeldtStilling.copy(stillingsId = UUID.fromString(stillingsid.asString()), innhold = enDirektemeldtStilling.innhold.copy(privacy = "SHOW_ALL"))
        val frontendStilling = enStilling.copy(uuid = stillingsid.toString())
        val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
            stillingsinfo = stillingsinfo.asStillingsinfoDto(),
            stilling = frontendStilling
        )

        whenever(stillingsinfoService.hentStillingsinfo(stillingsid)).thenReturn(stillingsinfo)
        whenever(direktemeldtStillingService.hentDirektemeldtStilling(stillingsid)).thenReturn(direktemeldtStilling)
        whenever(direktemeldtStillingService.hentDirektemeldtStilling(stillingsid.verdi)).thenReturn(direktemeldtStilling)

        stillingService.oppdaterRekrutteringsbistandStilling(rekrutteringsbistandStilling, eier)

        verify(times(0)) { stillingOutboxService.lagreMeldingIOutbox(
            any(), any()) }
    }

    @Test
    fun `Skal ikke kunne kopiere stilling med stillingskategori REKRUTTERINGSTREFF_FORMIDLING`() {
        val stillingsid = UUID.randomUUID()
        val eier = Eier(navident = "Z123456", navn = "Ola Nordmann", navKontorEnhetId = "1234")
        val stillingsinfo = Stillingsinfo(
            stillingsid = Stillingsid(stillingsid),
            stillingsinfoid = Stillingsinfoid("123e4567-e89b-12d3-a456-426614174001"),
            eier = eier,

            stillingskategori = Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING,
            )
        val direktemeldtStilling = enDirektemeldtStilling.copy(stillingsId = stillingsid, innhold = enDirektemeldtStilling.innhold.copy(privacy = "SHOW_ALL"))

        whenever(stillingsinfoService.hentStillingsinfo(Stillingsid(stillingsid))).thenReturn(stillingsinfo)
        whenever(direktemeldtStillingService.hentDirektemeldtStilling(stillingsid)).thenReturn(direktemeldtStilling)

        assertThrows<ResponseStatusException> {
            stillingService.kopierStilling(stillingsid, eier.navident, eier.navn, eier.navKontorEnhetId)
        }
    }

    @Test
    fun `Skal ikke kunne kopiere stilling med stillingskategori FORMIDLING `() {
        val stillingsid = UUID.randomUUID()
        val eier = Eier(navident = "Z123456", navn = "Ola Nordmann", navKontorEnhetId = "1234")
        val stillingsinfo = Stillingsinfo(
            stillingsid = Stillingsid(stillingsid),
            stillingsinfoid = Stillingsinfoid("123e4567-e89b-12d3-a456-426614174001"),
            eier = eier,

            stillingskategori = Stillingskategori.FORMIDLING,
        )
        val direktemeldtStilling = enDirektemeldtStilling.copy(stillingsId = stillingsid, innhold = enDirektemeldtStilling.innhold.copy(privacy = "SHOW_ALL"))

        whenever(stillingsinfoService.hentStillingsinfo(Stillingsid(stillingsid))).thenReturn(stillingsinfo)
        whenever(direktemeldtStillingService.hentDirektemeldtStilling(stillingsid)).thenReturn(direktemeldtStilling)

        assertThrows<ResponseStatusException> {
            stillingService.kopierStilling(stillingsid, eier.navident, eier.navn, eier.navKontorEnhetId)
        }
    }
}
