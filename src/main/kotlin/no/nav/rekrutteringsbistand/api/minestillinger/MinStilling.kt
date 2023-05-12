package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import java.time.ZonedDateTime

data class MinStilling(
    val stillingsId: Stillingsid,
    val sistEndret: ZonedDateTime,
    val tittel: String,
    val annonsenr: Long,
    val arbeidsgiverNavn: String,
    val utløpsdato: ZonedDateTime,
    val status: String
)
