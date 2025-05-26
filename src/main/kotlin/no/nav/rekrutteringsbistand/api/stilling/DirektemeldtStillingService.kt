package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stilling.outbox.EventName
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.stereotype.Service

@Service
class DirektemeldtStillingService(
    private val direktemeldtStillingRepository: DirektemeldtStillingRepository,
    private val stillingOutboxService: StillingOutboxService
) {
    fun lagreDirektemeldtStilling(direktemeldtStilling: DirektemeldtStilling) {
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)

        stillingOutboxService.lagreMeldingIOutbox(
            stillingsId = direktemeldtStilling.stillingsId,
            eventName = EventName.INDEKSER_DIREKTEMELDT_STILLING
        )
    }

    fun hentDirektemeldtStilling(stillingsId: String): DirektemeldtStilling? {
        return direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)
    }

    fun hentDirektemeldtStilling(stillingsId: Stillingsid) : DirektemeldtStilling? {
        return hentDirektemeldtStilling(stillingsId.asString())
    }

    fun hentAlleDirektemeldteStillinger(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentAlleDirektemeldteStillinger()
    }

    fun hentStillingerForAktivering(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentStillingerForAktivering()
    }

    fun hentStillingerForDeaktivering(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentStillingerForDeaktivering()
    }

    fun hentUtgåtteStillingerFor6mndSidenSomErPending(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentUtgåtteStillingerFor6mndSidenSomErPending()
    }
}
