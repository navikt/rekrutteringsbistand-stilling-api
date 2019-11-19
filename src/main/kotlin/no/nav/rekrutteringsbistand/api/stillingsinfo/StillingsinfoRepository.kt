package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.Option
import arrow.core.firstOrNone
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class StillingsinfoRepository(
        val jdbcTemplate: NamedParameterJdbcTemplate,
        simpleJdbcInsert: SimpleJdbcInsert) {

    val stillingsinfoInsert = simpleJdbcInsert.withTableName("Stillingsinfo").usingGeneratedKeyColumns("ID")

    fun lagre(stillingsinfo: Stillingsinfo) =
            stillingsinfoInsert.executeAndReturnKey(
                    mapOf(
                            "stillingsinfoid" to stillingsinfo.stillingsinfoid.asString(),
                            "stillingsid" to stillingsinfo.stillingsid.asString(),
                            "eier_navident" to stillingsinfo.eier.navident,
                            "eier_navn" to stillingsinfo.eier.navn
                    )
            )

    fun oppdaterEierIdentOgEierNavn(oppdatering: OppdaterStillingsinfo) =
            jdbcTemplate.update(
                    "update Stillingsinfo set eier_navident=:eier_navident, eier_navn=:eier_navn where stillingsinfoid=:stillingsinfoid",
                    mapOf(
                            "stillingsinfoid" to oppdatering.stillingsinfoid.asString(),
                            "eier_navident" to oppdatering.eier.navident,
                            "eier_navn" to oppdatering.eier.navn
                    )

            )

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> {
        val list = hentForStillinger(listOf(stillingId))
        check(list.size <= 1) { "Antall stillingsinfo for stillingsid ${stillingId.asString()}: ${list.size}" }
        return list.firstOrNone()
    }


    fun hentForStillinger(stillingsider: List<Stillingsid>): List<Stillingsinfo> =
            jdbcTemplate.query(
                    "SELECT * FROM STILLINGSINFO WHERE STILLINGSID IN(:stillingsider)",
                    MapSqlParameterSource("stillingsider", stillingsider.map { it.asString() }.joinToString(",")))
            { rs: ResultSet, _: Int ->
                Stillingsinfo.fromDB(rs)
            }

    fun hentForIdent(ident: String): Collection<Stillingsinfo> =
            jdbcTemplate.query(
                    "SELECT * FROM STILLINGSINFO WHERE eier_navident = :eier_navident",
                    MapSqlParameterSource("eier_navident", ident))
            { rs: ResultSet, _: Int ->
                Stillingsinfo.fromDB(rs)
            }

    fun slett(stillingsinfoid: Stillingsinfoid) =
            jdbcTemplate.update("DELETE FROM STILLINGSINFO WHERE STILLINGSINFOID = :stillingsinfoid",
                    MapSqlParameterSource("stillingsinfoid", stillingsinfoid.asString()))
}
