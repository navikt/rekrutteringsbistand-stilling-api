package no.nav.rekrutteringsbistand.api.inkludering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.minidev.json.JSONArray
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet


@Repository
class InkluderingRepository(
        val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val objectMapper = ObjectMapper()
    val simpleJdbcInsert = SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate).withTableName(inkluderingsmuligheterTabell).usingGeneratedKeyColumns("id")

    fun lagreInkludering(inkluderingsmulighet: Inkluderingsmulighet): Number {
        val retur = simpleJdbcInsert.executeAndReturnKey(
                mapOf(
                        stillingsidFelt to inkluderingsmulighet.stillingsid,
                        tilretteleggingmuligheterFelt to objectMapper.writeValueAsString(inkluderingsmulighet.tilretteleggingmuligheter),
                        virkemidlerFelt to objectMapper.writeValueAsString(inkluderingsmulighet.virkemidler),
                        prioriterteMålgrupperFelt to objectMapper.writeValueAsString(inkluderingsmulighet.prioriterte_maalgrupper),
                        statligInkluderingsdugnadFelt to inkluderingsmulighet.statlig_inkluderingsdugnad
                )
        )
        LOG.info("Lagret stilling med dbid:$retur og stillingsid:${inkluderingsmulighet.stillingsid}")
        return retur
    }

    fun hentInkluderingForStillingId(stillingId: String): List<Inkluderingsmulighet> {
        return namedJdbcTemplate.query(
                //"SELECT * FROM $inkluderingsmuligheterTabell WHERE ${stillingsidFelt} = '$stillingId'",
                "SELECT * FROM $inkluderingsmuligheterTabell"
                //, MapSqlParameterSource("stillingsid", stillingId)
        )
        { rs: ResultSet, _: Int ->
            Inkluderingsmulighet(
                    stillingsid = rs.getString(stillingsidFelt),
                    tilretteleggingmuligheter = tilStringListe(rs.getString(tilretteleggingmuligheterFelt)),
                    virkemidler = tilStringListe(rs.getString(virkemidlerFelt)),
                    prioriterte_maalgrupper = tilStringListe(rs.getString(prioriterteMålgrupperFelt)),
                    statlig_inkluderingsdugnad = rs.getBoolean(statligInkluderingsdugnadFelt)
            )
        }
    }

    private fun tilStringListe(string: String): List<String> {
        return ObjectMapper().readValue(string)
    }


    companion object {
        val inkluderingsmuligheterTabell = "inkluderingsmuligheter"
        const val idFelt = "id"
        const val stillingsidFelt = "stillingsid"
        const val tilretteleggingmuligheterFelt = "tilretteleggingmuligheter"
        const val virkemidlerFelt = "virkemidler"
        const val prioriterteMålgrupperFelt = "prioriterte_maalgrupper"
        const val statligInkluderingsdugnadFelt = "statlig_inkluderingsdugnad"
        const val radOpprettetFelt = "rad_opprettet"
    }
}
