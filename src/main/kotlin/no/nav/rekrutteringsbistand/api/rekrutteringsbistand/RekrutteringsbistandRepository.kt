package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

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
                            Pair("rekruttering_uuid", rekrutteringsbistand.rekrutteringUuid),
                            Pair("stilling_uuid", rekrutteringsbistand.stillingUuid),
                            Pair("eier_ident", rekrutteringsbistand.eierIdent),
                            Pair("eier_navn", rekrutteringsbistand.eierNavn)
                    )
            )

    fun oppdaterEierIdentOgEierNavn(oppdatering: OppdaterRekrutteringsbistand) =
            jdbcTemplate.update(
                    "update REKRUTTERINGSBISTAND set eier_ident=:eier_ident, eier_navn=:eier_navn where rekruttering_uuid=:rekruttering_uuid",
                    mapOf(
                            Pair("rekruttering_uuid", oppdatering.rekrutteringsUuid),
                            Pair("eier_ident", oppdatering.eierIdent),
                            Pair("eier_navn", oppdatering.eierNavn)
                    )

            )

    fun hentForStilling(stillingUuid: String): Rekrutteringsbistand =
            jdbcTemplate.queryForObject(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid = :stilling_uuid",
                    MapSqlParameterSource("stilling_uuid", stillingUuid))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }!!

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