package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import arrow.core.Option
import arrow.core.firstOrNone
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class RekrutteringsbistandRepository(
        val jdbcTemplate: NamedParameterJdbcTemplate,
        simpleJdbcInsert: SimpleJdbcInsert) {

    val rekrutteringsbistandInsert = simpleJdbcInsert.withTableName("REKRUTTERINGSBISTAND").usingGeneratedKeyColumns("ID")

    fun lagre(rekrutteringsbistand: Rekrutteringsbistand) =
            rekrutteringsbistandInsert.executeAndReturnKey(
                    mapOf(
                            "rekruttering_uuid" to  rekrutteringsbistand.rekrutteringUuid,
                            "stilling_uuid" to rekrutteringsbistand.stillingUuid,
                            "eier_ident" to rekrutteringsbistand.eierIdent,
                            "eier_navn" to rekrutteringsbistand.eierNavn
                    )
            )

    fun oppdaterEierIdentOgEierNavn(oppdatering: OppdaterRekrutteringsbistand) =
            jdbcTemplate.update(
                    "update REKRUTTERINGSBISTAND set eier_ident=:eier_ident, eier_navn=:eier_navn where rekruttering_uuid=:rekruttering_uuid",
                    mapOf(
                            "rekruttering_uuid" to oppdatering.rekrutteringsUuid,
                            "eier_ident" to oppdatering.eierIdent,
                            "eier_navn" to oppdatering.eierNavn
                    )

            )

    fun hentForStilling(stillingUuid: String): Option<Rekrutteringsbistand> {
        val list = hentForStillinger(listOf(stillingUuid))
        check(list.size <= 1) { "Ant. Rekrutteringsbistand for stillingUuid $stillingUuid: ${list.size}" }
        return list.firstOrNone()
    }


    fun hentForStillinger(stillingUuider: List<String>): List<Rekrutteringsbistand> =
            jdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid IN(:stilling_uuider)",
                    MapSqlParameterSource("stilling_uuider", stillingUuider.joinToString(",")))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }

    fun hentForIdent(ident: String): Collection<Rekrutteringsbistand> =
            jdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE eier_ident = :eier_ident",
                    MapSqlParameterSource("eier_ident", ident))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }

    fun slett(rekrutteringsUuid: String) =
            jdbcTemplate.update("DELETE FROM REKRUTTERINGSBISTAND WHERE rekruttering_uuid = :rekruttering_uuid",
                    MapSqlParameterSource("rekruttering_uuid", rekrutteringsUuid))
}

data class OppdaterRekrutteringsbistand(val rekrutteringsUuid: String, val eierIdent: String, val eierNavn: String)
