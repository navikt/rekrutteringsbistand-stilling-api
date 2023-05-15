package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class MineStillingerRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val simpleJdbcInsert =
        SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate).withTableName("min_stilling").usingGeneratedKeyColumns("id")

    fun opprett(minStilling: MinStilling) {

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

    fun oppdater(minStilling: MinStilling) {
        val params = mapOf(
            "tittel" to minStilling.tittel,
            "sistEndret" to minStilling.sistEndret,
            "arbeidsgiverNavn" to minStilling.arbeidsgiverNavn,
            "utløpsdato" to minStilling.utløpsdato,
            "status" to minStilling.status,
            "eierNavIdent" to minStilling.eierNavIdent,
        )

        namedJdbcTemplate.update(
            """
        update min_stilling set
        tittel = :tittel,
        sist_endret = :sistEndret,
        arbeidsgiver_navn = :arbeidsgiverNavn,
        utløpsdato = :utløpsdato,
        status = :status,
        eier_nav_ident = :eierNavIdent
            """.trimIndent(),
            params
        )
    }

    fun hentForNavIdent(navIdent: String): List<MinStilling> {
        val sql = "select * from min_stilling where eier_nav_ident = :ident order by sist_endret"
        val params = MapSqlParameterSource("ident", navIdent)

        return namedJdbcTemplate.query(sql, params) { rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }
    }

    fun hentForStillingsId(stillingsId: Stillingsid): MinStilling? {
        val sql = "select * from min_stilling where stillingsid = :stillingsid"
        val params = MapSqlParameterSource("stillingsid", stillingsId)

        val stillinger = namedJdbcTemplate.query(sql, params) { rs: ResultSet, _: Int ->
            MinStilling.fromDB(rs)
        }

        if (stillinger.size > 1) {
            throw Exception("Mer enn én stilling med UUID $stillingsId")
        }
        return stillinger.firstOrNull()
    }
}
