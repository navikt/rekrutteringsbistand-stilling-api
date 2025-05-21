package no.nav.rekrutteringsbistand.api.stillingsinfo

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
                NOTAT to null,
                STILLINGSKATEGORI to stillingsinfo.stillingskategori?.name,
                EIER_NAVKONTOR_ENHETID to stillingsinfo.eier?.navKontorEnhetId,
            )
        )
    }

    fun slett(stillingsid: Stillingsid) = slett(stillingsid.asString())

    fun slett(stillingsid: String) =
        namedJdbcTemplate.update(
            "delete from $STILLINGSINFO where $STILLINGSID=:stillingsid",
            mapOf(
                "stillingsid" to stillingsid
            )
        )

    fun oppdaterEier(stillingsinfoId: Stillingsinfoid, nyEier: Eier?) =
        namedJdbcTemplate.update(
            "update $STILLINGSINFO set $EIER_NAVIDENT=:eier_navident, $EIER_NAVN=:eier_navn, $EIER_NAVKONTOR_ENHETID=:eier_navkontor_enhetid where $STILLINGSINFOID=:stillingsinfoid",
            mapOf(
                "stillingsinfoid" to stillingsinfoId.asString(),
                "eier_navident" to nyEier?.navident,
                "eier_navn" to nyEier?.navn,
                "eier_navkontor_enhetid" to nyEier?.navKontorEnhetId,
            )
        )

    fun oppdaterNavKontorEnhetId(stillingsinfoId: Stillingsinfoid, navKontorEnhetId: String) =
        namedJdbcTemplate.update(
            "update $STILLINGSINFO set $EIER_NAVKONTOR_ENHETID=:eier_navkontor_enhetid where $STILLINGSINFOID=:stillingsinfoid",
            mapOf(
                "stillingsinfoid" to stillingsinfoId.asString(),
                "eier_navkontor_enhetid" to navKontorEnhetId,
            )
        )

    fun hentForStilling(stillingId: Stillingsid): Stillingsinfo? {
        val list = hentForStillinger(listOf(stillingId))
        check(list.size <= 1) { "Antall stillingsinfo for stillingsid ${stillingId.asString()}: ${list.size}" }
        return list.firstOrNull()
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

    companion object {
        const val STILLINGSINFO = "Stillingsinfo"
        const val STILLINGSINFOID = "stillingsinfoid"
        const val STILLINGSID = "stillingsid"
        const val EIER_NAVIDENT = "eier_navident"
        const val EIER_NAVN = "eier_navn"
        const val NOTAT = "notat"
        const val STILLINGSKATEGORI = "stillingskategori"
        const val EIER_NAVKONTOR_ENHETID = "eier_navkontor_enhetid"
    }
}
