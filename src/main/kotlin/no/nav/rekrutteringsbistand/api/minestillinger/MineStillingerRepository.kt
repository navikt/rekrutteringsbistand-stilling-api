package no.nav.rekrutteringsbistand.api.minestillinger

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class MineStillingerRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val simpleJdbcInsert = SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate).withTableName("min_stilling").usingGeneratedKeyColumns("id")

    fun lagre(minStilling: MinStilling) {
        simpleJdbcInsert.execute(
            mapOf(
                "stillingsid" to minStilling.stillingsId.verdi,
                "tittel" to minStilling.tittel,
                "sist_endret" to minStilling.sistEndret,
                "annonsenr" to minStilling.annonsenr,
                "arbeidsgiver_navn" to minStilling.arbeidsgiverNavn,
                "utløpsdato" to minStilling.utløpsdato,
                "status" to minStilling.status,
                "eier_nav_ident" to minStilling.eierNavIdent
            )
        )
    }

    fun hent(navIdent: String): List<MinStilling> {
        val sql = "select * from min_stilling where eier_nav_ident = :ident order by sist_endret"
        val params = MapSqlParameterSource("ident", navIdent)

        return namedJdbcTemplate.query(sql, params) {
            rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }
    }

    fun hentAlle(): List<MinStilling> {
        val sql = "select * from min_stilling"

        return namedJdbcTemplate.query(sql) {
                rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }
    }
}
