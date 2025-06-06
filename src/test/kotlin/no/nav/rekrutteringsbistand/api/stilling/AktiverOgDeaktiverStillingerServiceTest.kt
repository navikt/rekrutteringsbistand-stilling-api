package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.Testdata.publishedFor2TimerSiden
import no.nav.rekrutteringsbistand.api.stillingStatusoppdatering.AktiverOgDeaktiverStillingerService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AktiverStillingerServiceTest {

    @Mock
    lateinit var direktemeldtStillingService: DirektemeldtStillingService

    lateinit var aktiverOgDeaktiverStillingerService: AktiverOgDeaktiverStillingerService

    val stillingCaptor = argumentCaptor<DirektemeldtStilling>()

    @BeforeEach
    fun setUp() {
        aktiverOgDeaktiverStillingerService = AktiverOgDeaktiverStillingerService(direktemeldtStillingService)
    }

    @Test
    fun `Skal kalle lagreDirektemeldtStilling stilling med status INACTIVE`() {
        val stilling = enDirektemeldtStilling

        whenever(direktemeldtStillingService.hentStillingerForDeaktivering()).thenReturn(listOf(stilling))
        whenever(direktemeldtStillingService.hentStillingerForAktivering()).thenReturn(listOf())

        aktiverOgDeaktiverStillingerService.deaktiverStillinger()

        verify(direktemeldtStillingService).lagreDirektemeldtStilling(stillingCaptor.capture())
        val capturedStilling = stillingCaptor.firstValue

        assertEquals(Status.INACTIVE.toString(), capturedStilling.status)
    }

    @Test
    fun `Skal kalle lagreDirektemeldtStilling stilling med status ACTIVE`() {
        val stilling = enDirektemeldtStilling.copy(
            status = Status.INACTIVE.toString(),
            publisert = publishedFor2TimerSiden,
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10),
            publisertAvAdmin = publishedFor2TimerSiden.toString()
        )

        whenever(direktemeldtStillingService.hentStillingerForDeaktivering()).thenReturn(listOf())
        whenever(direktemeldtStillingService.hentStillingerForAktivering()).thenReturn(listOf(stilling))

        aktiverOgDeaktiverStillingerService.aktiverStillinger()

        verify(direktemeldtStillingService).lagreDirektemeldtStilling(stillingCaptor.capture())
        val capturedStilling = stillingCaptor.firstValue

        assertEquals(Status.ACTIVE.toString(), capturedStilling.status)
    }
}
