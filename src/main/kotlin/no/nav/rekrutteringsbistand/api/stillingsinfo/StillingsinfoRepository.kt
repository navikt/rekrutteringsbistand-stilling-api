package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.optionOf
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class StillingsinfoRepository(
    val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val simpleJdbcInsert =
        SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate).withTableName("Stillingsinfo").usingGeneratedKeyColumns("id")

    fun opprett(stillingsinfo: Stillingsinfo) {
        simpleJdbcInsert.executeAndReturnKey(
            mapOf(
                STILLINGSINFOID to stillingsinfo.stillingsinfoid.asString(),
                STILLINGSID to stillingsinfo.stillingsid.asString(),
                EIER_NAVIDENT to stillingsinfo.eier?.navident,
                EIER_NAVN to stillingsinfo.eier?.navn,
                NOTAT to stillingsinfo.notat,
                STILLINGSKATEGORI to stillingsinfo.stillingskategori?.name
            )
        )
    }

    fun slett(stillingsid: String) =
        namedJdbcTemplate.update(
            "delete from $STILLINGSINFO where $STILLINGSID=:stillingsid",
            mapOf(
                "stillingsid" to stillingsid
            )
        )

    fun oppdaterEier(stillingsinfoId: Stillingsinfoid, nyEier: Eier) =
        namedJdbcTemplate.update(
            "update $STILLINGSINFO set $EIER_NAVIDENT=:eier_navident, $EIER_NAVN=:eier_navn where $STILLINGSINFOID=:stillingsinfoid",
            mapOf(
                "stillingsinfoid" to stillingsinfoId.asString(),
                "eier_navident" to nyEier.navident,
                "eier_navn" to nyEier.navn
            )
        )

    fun oppdaterNotat(oppdatering: OppdaterNotat) {
        namedJdbcTemplate.update(
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
        return optionOf(list.firstOrNull())
    }

    fun hentForStillinger(stillingsider: List<Stillingsid>): List<Stillingsinfo> {
        if (stillingsider.isEmpty()) {
            return emptyList()
        }

        val sql = "SELECT * FROM $STILLINGSINFO WHERE $STILLINGSID IN(:stillingsider) ORDER BY ID"
        val params = MapSqlParameterSource("stillingsider", stillingsider.map { it.asString() })

        return namedJdbcTemplate.query(sql, params)
        { rs: ResultSet, _: Int ->
            Stillingsinfo.fromDB(rs)
        }
    }

    fun hentForIdent(ident: String): List<Stillingsinfo> =
        namedJdbcTemplate.query(
            "SELECT * FROM $STILLINGSINFO WHERE $EIER_NAVIDENT = :eier_navident",
            MapSqlParameterSource("eier_navident", ident)
        )
        { rs: ResultSet, _: Int ->
            Stillingsinfo.fromDB(rs)
        }

    companion object {
        const val STILLINGSINFO = "Stillingsinfo"
        const val STILLINGSINFOID = "stillingsinfoid"
        const val STILLINGSID = "stillingsid"
        const val EIER_NAVIDENT = "eier_navident"
        const val EIER_NAVN = "eier_navn"
        const val NOTAT = "notat"
        const val STILLINGSKATEGORI = "stillingskategori"
    }
}
