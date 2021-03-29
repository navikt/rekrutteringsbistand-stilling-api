package no.nav.rekrutteringsbistand.api.standardsøk

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class StandardsøkRepository(
        val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val simpleJdbcInsert = SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate).withTableName("lagret_sok").usingGeneratedKeyColumns("id")

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
        return simpleJdbcInsert.executeAndReturnKey(mapOf(
                "nav_ident" to navIdent,
                "sok" to lagreStandardsøkDto.søk,
                "tidspunkt" to LocalDateTime.now()
        )).toInt()
    }

    private fun endreStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, id: String): Int {
        return namedJdbcTemplate.update(
                "UPDATE lagret_sok SET sok = :sok, tidspunkt = :tidspunkt WHERE id = :id",
                MapSqlParameterSource(mapOf(
                        "sok" to lagreStandardsøkDto.søk,
                        "tidspunkt" to LocalDateTime.now(),
                        "id" to id.toBigInteger()
                )),
        )
    }

    fun hentStandardsøk(navIdent: String): LagretStandardsøk? {
        return namedJdbcTemplate.query(
                "SELECT * FROM lagret_sok WHERE nav_ident = :nav_ident",
                MapSqlParameterSource(mapOf(
                        "nav_ident" to navIdent
                ))
        ) { rs: ResultSet, _: Int ->
            LagretStandardsøk.fromDB(rs)
        }.firstOrNull()
    }
}
