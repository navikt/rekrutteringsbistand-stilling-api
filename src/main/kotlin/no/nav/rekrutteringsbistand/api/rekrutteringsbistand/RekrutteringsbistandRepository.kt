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
                            "rekruttering_uuid" to  rekrutteringsbistand.rekrutteringId.asString(),
                            "stilling_uuid" to rekrutteringsbistand.stillingId.asString(),
                            "eier_ident" to rekrutteringsbistand.eier.ident,
                            "eier_navn" to rekrutteringsbistand.eier.navn
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

    fun hentForStilling(stillingId: StillingId): Option<Rekrutteringsbistand> {
        val list = hentForStillinger(listOf(stillingId))
        check(list.size <= 1) { "Ant. Rekrutteringsbistand for stillingUuid ${stillingId.asString()}: ${list.size}" }
        return list.firstOrNone()
    }


    fun hentForStillinger(stillingIder: List<StillingId>): List<Rekrutteringsbistand> =
            jdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid IN(:stilling_uuider)",
                    MapSqlParameterSource("stilling_uuider", stillingIder.map { it.asString() }.joinToString(",")))
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

    fun slett(rekrutteringId: RekrutteringId) =
            jdbcTemplate.update("DELETE FROM REKRUTTERINGSBISTAND WHERE rekruttering_uuid = :rekruttering_uuid",
                    MapSqlParameterSource("rekruttering_uuid", rekrutteringId.asString()))
}

data class OppdaterRekrutteringsbistand(val rekrutteringsUuid: String, val eierIdent: String, val eierNavn: String)
