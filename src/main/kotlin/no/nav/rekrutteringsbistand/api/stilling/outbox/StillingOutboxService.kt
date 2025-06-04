package no.nav.rekrutteringsbistand.api.stilling.outbox

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StillingOutboxService(
    private val stillingOutboxRepository: StillingOutboxRepository
) {

    fun lagreMeldingIOutbox(stillingsId: UUID, eventName: EventName) {
        stillingOutboxRepository.lagreMeldingIOutbox(stillingsId, eventName)
    }
}
