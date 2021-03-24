package no.nav.rekrutteringsbistand.api.standardsøk

import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class StandardsøkRepository(
        val jdbcTemplate: NamedParameterJdbcTemplate,
        val simpleJdbcInsert: SimpleJdbcInsert
) {
    fun oppdaterStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, navIdent: String): LagretStandardsøk? {
        val lagretStandardsøk = hentStandardsøk(navIdent)
        if (lagretStandardsøk == null) {
            opprettStandardsøk(lagreStandardsøkDto, navIdent)
        } else {
            endreStandardsøk(lagreStandardsøkDto, lagretStandardsøk.id)
        }

        return hentStandardsøk(navIdent)
    }

    private fun opprettStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, navIdent: String): Int {
        val insert = simpleJdbcInsert
                .withTableName("lagret_sok")
                .usingGeneratedKeyColumns("id")

        return insert.executeAndReturnKey(mapOf(
                "nav_ident" to navIdent,
                "sok" to lagreStandardsøkDto.søk,
                "tidspunkt" to LocalDateTime.now()
        )).toInt()
    }

    private fun endreStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, id: String): Int {
        return jdbcTemplate.update(
                "UPDATE lagret_sok SET sok = :sok WHERE id = :id",
                MapSqlParameterSource(mapOf(
                        "sok" to lagreStandardsøkDto.søk,
                        "id" to id
                )),
        )
    }

    fun hentStandardsøk(navIdent: String): LagretStandardsøk? {
        return jdbcTemplate.query(
                "SELECT * FROM lagret_sok WHERE nav_ident = :nav_ident",
                MapSqlParameterSource(mapOf(
                        "nav_ident" to navIdent
                ))
        ) { rs: ResultSet, _: Int ->
            LagretStandardsøk.fromDB(rs)
        }.firstOrNull()
    }
}
