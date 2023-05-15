package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.sql.ResultSet
import java.time.ZoneId
import java.time.ZonedDateTime

data class MinStilling(
    val stillingsId: Stillingsid,
    val sistEndret: ZonedDateTime,
    val tittel: String,
    val annonsenr: Long,
    val arbeidsgiverNavn: String,
    val utløpsdato: ZonedDateTime,
    val status: String,
    val eierNavIdent: String,
) {
    companion object {
        fun fromDB(rs: ResultSet) =
            MinStilling(
                stillingsId = Stillingsid(rs.getString("stillingsid")),
                sistEndret = rs.getZonedDateTime("sist_endret"),
                tittel = rs.getString("tittel"),
                annonsenr = rs.getLong("annonsenr"),
                arbeidsgiverNavn = rs.getString("arbeidsgiver_navn"),
                utløpsdato = rs.getZonedDateTime("utløpsdato"),
                status = rs.getString("status"),
                eierNavIdent = rs.getString("eier_nav_ident")

            )

        fun fromStilling(stilling: Stilling, eierNavIdent: String) =
            MinStilling(
                stillingsId = Stillingsid(stilling.uuid),
                sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                tittel = stilling.title,
                annonsenr = stilling.id,
                arbeidsgiverNavn = stilling.businessName!!, // TODO: Håndter dette
                utløpsdato = stilling.expires!!.atZone(ZoneId.of("Europe/Oslo")), // TODO: Håndter dette
                status = stilling.status,
                eierNavIdent = eierNavIdent
            )
    }
}

private fun ResultSet.getZonedDateTime(columnLabel: String) =
    ZonedDateTime.ofInstant(this.getTimestamp(columnLabel).toInstant(), ZoneId.of("Europe/Oslo"))
