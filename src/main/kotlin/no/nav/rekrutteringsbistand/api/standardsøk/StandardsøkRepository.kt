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
        simpleJdbcInsert: SimpleJdbcInsert
) {
    val standardsøkInsert = simpleJdbcInsert.withTableName("lagret_sok").usingGeneratedKeyColumns("id")

    fun oppdaterStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, navIdent: String): LagretStandardsøk {
        val lagretStandardsøk = hentStandardsøk(navIdent)

        if (lagretStandardsøk != null) {
            return lagretStandardsøk
        }

        lagreStandardsøk(lagreStandardsøkDto, navIdent)
        val oppdatertStandardsøk = hentStandardsøk(navIdent)

        if (oppdatertStandardsøk != null) {
            return oppdatertStandardsøk
        }

        throw Exception("Kunne ikke lagre standardsøk")
    }

    private fun lagreStandardsøk(lagreStandardsøkDto: LagreStandardsøkDto, navIdent: String) {
        val id = standardsøkInsert.executeAndReturnKey(mapOf(
                "nav_ident" to navIdent,
                "sok" to lagreStandardsøkDto.søk,
                "tidspunkt" to LocalDateTime.now(),
        ))

        LOG.info("Lagret med id $id")
    }

    fun hentStandardsøk(navIdent: String): LagretStandardsøk? {
        return jdbcTemplate.query(
                "SELECT * FROM lagret_sok WHERE nav_ident = :nav_ident LIMIT 1",
                MapSqlParameterSource("nav_ident", navIdent)
        ) {
            rs: ResultSet, _: Int -> LagretStandardsøk(
                    id = rs.getString("id"),
                    søk = rs.getString("sok"),
                    navIdent = rs.getString("nav_ident"),
                    tidspunkt = rs.getTimestamp("tidspunkt").toLocalDateTime()
            )
        }.firstOrNull()
    }
}
