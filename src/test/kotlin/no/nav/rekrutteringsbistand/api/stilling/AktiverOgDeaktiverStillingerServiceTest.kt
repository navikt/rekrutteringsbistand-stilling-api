package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.Testdata.publishedFor2TimerSiden
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
class AktiverOgDeaktiverStillingerServiceTest {

    @Mock
    lateinit var direktemeldtStillingRepository: DirektemeldtStillingRepository

    lateinit var aktiverOgDeaktiverStillingerService: AktiverOgDeaktiverStillingerService

    val stillingCaptor = argumentCaptor<DirektemeldtStilling>()

    @BeforeEach
    fun setUp() {
        aktiverOgDeaktiverStillingerService = AktiverOgDeaktiverStillingerService(direktemeldtStillingRepository)
    }

    @Test
    fun `Skal lagre stilling med status inaktiv`() {
        val stilling = enDirektemeldtStilling

        whenever(direktemeldtStillingRepository.hentStillingerForDeaktivering()).thenReturn(listOf(stilling))
        whenever(direktemeldtStillingRepository.hentStillingerForAktivering()).thenReturn(listOf())

        aktiverOgDeaktiverStillingerService.aktiverOgDeaktiverStillinger()

        verify(direktemeldtStillingRepository).lagreDirektemeldtStilling(stillingCaptor.capture())
        val capturedStilling = stillingCaptor.firstValue

        assertEquals(Status.INACTIVE.toString(), capturedStilling.status)
    }

    @Test
    fun `Skal lagre stilling med status aktiv`() {
        val stilling = enDirektemeldtStilling.copy(status = Status.INACTIVE.toString(),
            innhold = enDirektemeldtStilling.innhold.copy(published = publishedFor2TimerSiden, expires = ZonedDateTime.now(
                ZoneId.of("Europe/Oslo")).plusDays(10), publishedByAdmin = publishedFor2TimerSiden.toString()))

        whenever(direktemeldtStillingRepository.hentStillingerForDeaktivering()).thenReturn(listOf())
        whenever(direktemeldtStillingRepository.hentStillingerForAktivering()).thenReturn(listOf(stilling))

        aktiverOgDeaktiverStillingerService.aktiverOgDeaktiverStillinger()

        verify(direktemeldtStillingRepository).lagreDirektemeldtStilling(stillingCaptor.capture())
        val capturedStilling = stillingCaptor.firstValue

        assertEquals(Status.ACTIVE.toString(), capturedStilling.status)
    }
}
