package no.nav.rekrutteringsbistand.api.stilling.outbox

import java.util.UUID

data class StillingOutboxMelding(
    val id: Long,
    val stillingsId: UUID,
    val eventName: EventName,
)

enum class EventName(name: String) {
    REINDEKSER_DIREKTEMELDT_STILLING("reindekserDirektemeldtStilling"),
    INDEKSER_DIREKTEMELDT_STILLING("indekserDirektemeldtStilling");

    override fun toString(): String {
        return name
    }
}
