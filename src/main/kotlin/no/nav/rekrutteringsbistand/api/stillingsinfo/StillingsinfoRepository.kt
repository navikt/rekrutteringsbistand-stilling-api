package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.option.Option
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class StillingsinfoRepository(
    val jdbcTemplate: NamedParameterJdbcTemplate,
    simpleJdbcInsert: SimpleJdbcInsert
) {

    val stillingsinfoInsert = simpleJdbcInsert.withTableName("Stillingsinfo").usingGeneratedKeyColumns("id")

    fun lagre(stillingsinfo: Stillingsinfo) =
        stillingsinfoInsert.executeAndReturnKey(
            mapOf(
                STILLINGSINFOID to stillingsinfo.stillingsinfoid.asString(),
                STILLINGSID to stillingsinfo.stillingsid.asString(),
                EIER_NAVIDENT to stillingsinfo.eier?.navident,
                EIER_NAVN to stillingsinfo.eier?.navn,
                NOTAT to stillingsinfo.notat
            )
        )

    fun oppdaterEierIdentOgEierNavn(oppdatering: OppdaterEier) =
        jdbcTemplate.update(
            "update $STILLINGSINFO set $EIER_NAVIDENT=:eier_navident, $EIER_NAVN=:eier_navn where $STILLINGSINFOID=:stillingsinfoid",
            mapOf(
                "stillingsinfoid" to oppdatering.stillingsinfoid.asString(),
                "eier_navident" to oppdatering.eier.navident,
                "eier_navn" to oppdatering.eier.navn
            )

        )

    fun oppdaterNotat(oppdatering: OppdaterNotat) {
        jdbcTemplate.update(
            "update $STILLINGSINFO set $NOTAT=:notat where $STILLINGSINFOID=:stillingsinfoid",
            mapOf(
                "stillingsinfoid" to oppdatering.stillingsinfoid.asString(),
                "notat" to oppdatering.notat
            )

        )
    }

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> {
        val list = hentForStillinger(listOf(stillingId))
        check(list.size <= 1) { "Antall stillingsinfo for stillingsid ${stillingId.asString()}: ${list.size}" }
        return Option(list.firstOrNull())
    }


    fun hentForStillinger(stillingsider: List<Stillingsid>): List<Stillingsinfo> =
        jdbcTemplate.query(
            "SELECT * FROM $STILLINGSINFO WHERE $STILLINGSID IN(:stillingsider)",
            MapSqlParameterSource("stillingsider", stillingsider.map { it.asString() }.joinToString(","))
        )
        { rs: ResultSet, _: Int ->
            Stillingsinfo.fromDB(rs)
        }

    fun hentForIdent(ident: String): Collection<Stillingsinfo> =
        jdbcTemplate.query(
            "SELECT * FROM $STILLINGSINFO WHERE $EIER_NAVIDENT = :eier_navident",
            MapSqlParameterSource("eier_navident", ident)
        )
        { rs: ResultSet, _: Int ->
            Stillingsinfo.fromDB(rs)
        }

    fun slett(stillingsinfoid: Stillingsinfoid) =
        jdbcTemplate.update(
            "DELETE FROM $STILLINGSINFO WHERE $STILLINGSINFOID = :stillingsinfoid",
            MapSqlParameterSource("stillingsinfoid", stillingsinfoid.asString())
        )

    companion object {
        const val STILLINGSINFO = "Stillingsinfo"
        const val STILLINGSINFOID = "stillingsinfoid"
        const val STILLINGSID = "stillingsid"
        const val EIER_NAVIDENT = "eier_navident"
        const val EIER_NAVN = "eier_navn"
        const val NOTAT = "notat"
    }
}
