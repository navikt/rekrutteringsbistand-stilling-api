package no.nav.rekrutteringsbistand.api.stilling.outbox

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ProsesserStillingOutboxJobbTest {

    private val stillingOutboxRepository = mock<StillingOutboxRepository>()
    private val direktemeldtStillingService = mock<DirektemeldtStillingService>()
    private val stillingsinfoService = mock<StillingsinfoService>()
    private val leaderElection = mock<LeaderElection> { on { isLeader() } doReturn true }
    private val rapidApplikasjon = mock<RapidApplikasjon>()

    private val jobb = ProsesserStillingOutboxJobb(
        stillingOutboxRepository,
        direktemeldtStillingService,
        stillingsinfoService,
        leaderElection,
        rapidApplikasjon
    )

    @Test
    fun `stillingsinfo sendes som JSON null når stilling mangler stillingsinfo ved indeksering av stillingsinfo`() {
        val stillingsId = UUID.randomUUID()
        whenever(stillingOutboxRepository.finnBatchMedUprossesertMeldinger())
            .thenReturn(listOf(StillingOutboxMelding(1, stillingsId, EventName.INDEKSER_STILLINGSINFO)))

        jobb.prosesserStillingOutboxJobb()

        val melding = publisertMelding()
        assertTrue(melding["stillingsinfo"].isNull, "stillingsinfo skal være JSON null, var: ${melding["stillingsinfo"]}")
        assertEquals(stillingsId.toString(), melding["stillingsId"].asText())
    }

    @Test
    fun `stillingsinfo sendes som JSON null når direktemeldt stilling mangler stillingsinfo`() {
        whenever(stillingOutboxRepository.finnBatchMedUprossesertMeldinger())
            .thenReturn(listOf(StillingOutboxMelding(1, enDirektemeldtStilling.stillingsId, EventName.INDEKSER_DIREKTEMELDT_STILLING)))
        whenever(direktemeldtStillingService.hentDirektemeldtStilling(any<UUID>())).thenReturn(enDirektemeldtStilling)

        jobb.prosesserStillingOutboxJobb()

        val melding = publisertMelding()
        assertTrue(melding["stillingsinfo"].isNull, "stillingsinfo skal være JSON null, var: ${melding["stillingsinfo"]}")
        assertTrue(melding["direktemeldtStilling"].isObject)
    }

    private fun publisertMelding() = argumentCaptor<JsonMessage>().let {
        verify(rapidApplikasjon).publish(any(), it.capture())
        jacksonObjectMapper().readTree(it.firstValue.toJson())
    }
}
