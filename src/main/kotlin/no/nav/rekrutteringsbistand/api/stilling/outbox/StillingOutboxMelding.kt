package no.nav.rekrutteringsbistand.api.stilling.outbox

import java.util.UUID

data class StillingOutboxMelding(
    val id: Long,
    val stillingsId: UUID,
    val eventName: EventName,
)

enum class EventName(val value: String) {
    REINDEKSER_DIREKTEMELDT_STILLING("reindekserDirektemeldtStilling"),
    INDEKSER_DIREKTEMELDT_STILLING("indekserDirektemeldtStilling"),
    INDEKSER_STILLINGSINFO("indekserStillingsinfo"),
    PUBLISER_ELLER_AVPUBLISER_TIL_ARBEIDSPLASSEN("publiserEllerAvpubliserTilArbeidsplassen");

    override fun toString(): String {
        return value
    }

    companion object {
        fun fromString(value: String): EventName {
            return when(value) {
                "reindekserDirektemeldtStilling" -> REINDEKSER_DIREKTEMELDT_STILLING
                "indekserDirektemeldtStilling" -> INDEKSER_DIREKTEMELDT_STILLING
                "indekserStillingsinfo" -> INDEKSER_STILLINGSINFO
                "publiserEllerAvpubliserTilArbeidsplassen" -> PUBLISER_ELLER_AVPUBLISER_TIL_ARBEIDSPLASSEN
                else -> throw IllegalArgumentException("Ugyldig event name: $value")
            }
        }
    }
}
