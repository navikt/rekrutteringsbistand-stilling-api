package no.nav.rekrutteringsbistand.api.inkludering

import com.fasterxml.jackson.databind.ObjectMapper
import net.minidev.json.JSONArray
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.support.LOG
import org.objectweb.asm.TypeReference
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet


@Repository
class InkluderingRepository(
    val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun lagreInkluderingBatch(inkluderingsmuligheter: List<Inkluderingsmuligheter>) {
        LOG.info("Kaller lagreInkludering")
        namedJdbcTemplate.batchUpdate(
            """INSERT INTO $inkluderingsmuligheterTabell
                    ($stillingsid, $tilretteleggingmuligheter, $virkemidler, $prioriterte_maalgrupper, $statlig_inkluderingsdugnad, $rad_opprettet) 
                   VALUES
                    (:$stillingsid, :$tilretteleggingmuligheter, :$virkemidler, :$prioriterte_maalgrupper, :$statlig_inkluderingsdugnad, $rad_opprettet)
                """.trimMargin(),
            inkluderingsmuligheter.map {
                MapSqlParameterSource(
                    mapOf(
                        stillingsid to it.stillingsid,
                        tilretteleggingmuligheter to JSONArray.toJSONString(it.tilretteleggingmuligheter),
                        virkemidler to JSONArray.toJSONString(it.virkemidler),
                        prioriterte_maalgrupper to JSONArray.toJSONString(it.prioriterte_maalgrupper),
                        statlig_inkluderingsdugnad to it.statlig_inkluderingsdugnad
                    )
                )
            }.toTypedArray()
        )
    }

    fun hentInkluderingForStillingId(stillingId: String): Inkluderingsmuligheter? {
        namedJdbcTemplate.query(
                "SELECT * FROM $inkluderingsmuligheterTabell WHERE ${DbFelt.stillingsid} = '$stillingId'",
                MapSqlParameterSource("stillingsid", stillingId)
        )
        { rs: ResultSet, _: Int ->
            Inkluderingsmuligheter(
                stillingsid = rs.getString(stillingId),
                tilretteleggingmuligheter = listOf(),
                virkemidler = listOf(),
                prioriterte_maalgrupper = listOf(),
                statlig_inkluderingsdugnad = false
            )
        }

        return null
    }

    companion object DbFelt {
        val inkluderingsmuligheterTabell = "INKLUDERINGSMULIGHETER"
        const val id = "id"
        const val stillingsid = "stillingsid"
        const val tilretteleggingmuligheter = "tilretteleggingmuligheter"
        const val virkemidler = "virkemidler"
        const val prioriterte_maalgrupper = "prioriterte_maalgrupper"
        const val statlig_inkluderingsdugnad = "statlig_inkluderingsdugnad"
        const val rad_opprettet = "rad_opprettet"
    }
}
