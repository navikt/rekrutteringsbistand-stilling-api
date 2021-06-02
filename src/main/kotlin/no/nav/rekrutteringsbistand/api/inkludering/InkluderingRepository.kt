package no.nav.rekrutteringsbistand.api.inkludering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

data class Inkluderingsmulighet(
    val stillingsid: String,
    val tilretteleggingmuligheter: List<String>,
    val virkemidler: List<String>,
    val prioriterteMålgrupper: List<String>,
    val statligInkluderingsdugnad: Boolean,
    val radOpprettet: LocalDateTime,
)

@Repository
class InkluderingRepository(val namedJdbcTemplate: NamedParameterJdbcTemplate) {

    val objectMapper = ObjectMapper()
    val simpleJdbcInsert = SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate)
        .withTableName(inkluderingsmuligheterTabell)
        .usingGeneratedKeyColumns("id")

    fun lagreInkluderingsmuligheter(inkluderingsmulighet: Inkluderingsmulighet): Number {
        val retur = simpleJdbcInsert.executeAndReturnKey(
            mapOf(
                stillingsidFelt to inkluderingsmulighet.stillingsid,
                tilretteleggingmuligheterFelt to objectMapper.writeValueAsString(inkluderingsmulighet.tilretteleggingmuligheter),
                virkemidlerFelt to objectMapper.writeValueAsString(inkluderingsmulighet.virkemidler),
                prioriterteMålgrupperFelt to objectMapper.writeValueAsString(inkluderingsmulighet.prioriterteMålgrupper),
                statligInkluderingsdugnadFelt to inkluderingsmulighet.statligInkluderingsdugnad,
                radOpprettetFelt to inkluderingsmulighet.radOpprettet
            )
        )
        LOG.info("Lagret stilling med dbid: $retur og stillingsid: ${inkluderingsmulighet.stillingsid}")
        return retur
    }

    fun hentInkludering(stillingId: String): List<Inkluderingsmulighet> {
        return namedJdbcTemplate.query("SELECT * FROM $inkluderingsmuligheterTabell WHERE $stillingsidFelt = '$stillingId'")
        { rs: ResultSet, _: Int ->
            Inkluderingsmulighet(
                stillingsid = rs.getString(stillingsidFelt),
                tilretteleggingmuligheter = tilStringListe(rs.getString(tilretteleggingmuligheterFelt)),
                virkemidler = tilStringListe(rs.getString(virkemidlerFelt)),
                prioriterteMålgrupper = tilStringListe(rs.getString(prioriterteMålgrupperFelt)),
                statligInkluderingsdugnad = rs.getBoolean(statligInkluderingsdugnadFelt),
                radOpprettet = rs.getTimestamp(radOpprettetFelt).toLocalDateTime()
            )
        }
    }

    private fun tilStringListe(string: String): List<String> {
        return ObjectMapper().readValue(string)
    }


    companion object {
        const val inkluderingsmuligheterTabell = "inkluderingsmuligheter"
        const val idFelt = "id"
        const val stillingsidFelt = "stillingsid"
        const val tilretteleggingmuligheterFelt = "tilretteleggingmuligheter"
        const val virkemidlerFelt = "virkemidler"
        const val prioriterteMålgrupperFelt = "prioriterte_maalgrupper"
        const val statligInkluderingsdugnadFelt = "statlig_inkluderingsdugnad"
        const val radOpprettetFelt = "rad_opprettet"
    }
}
