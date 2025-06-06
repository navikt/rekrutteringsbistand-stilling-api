package no.nav.rekrutteringsbistand.api.stilling.outbox

import java.util.UUID

data class StillingOutboxMelding(
    val id: Long,
    val stillingsId: UUID,
    val eventName: EventName,
)

enum class EventName(val value: String) {
    REINDEKSER_DIREKTEMELDT_STILLING("reindekserDirektemeldtStilling"),
    INDEKSER_DIREKTEMELDT_STILLING("indekserDirektemeldtStilling");

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromString(value: String): EventName {
            return when(value) {
                "reindekserDirektemeldtStilling" -> REINDEKSER_DIREKTEMELDT_STILLING
                "indekserDirektemeldtStilling" -> INDEKSER_DIREKTEMELDT_STILLING
                else -> throw IllegalArgumentException("Ugyldig event name: $value")
            }
        }
    }
}
