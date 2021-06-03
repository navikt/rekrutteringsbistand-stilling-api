package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

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
        val tilretteleggingmuligheter: List<String> = emptyList(),
        val virkemidler: List<String> = emptyList(),
        val prioriterteMålgrupper: List<String> = emptyList(),
        val statligInkluderingsdugnad: Boolean = false,
        val radOpprettet: LocalDateTime
) {
    fun harInkludering(): Boolean =
            tilretteleggingmuligheter.isNotEmpty()
                    || virkemidler.isNotEmpty()
                    || prioriterteMålgrupper.isNotEmpty()
                    || statligInkluderingsdugnad

    fun erLik(other: Inkluderingsmulighet): Boolean =
            containsExactly(tilretteleggingmuligheter, other.tilretteleggingmuligheter) &&
                    containsExactly(virkemidler, other.virkemidler) &&
                    containsExactly(prioriterteMålgrupper, other.prioriterteMålgrupper) &&
                    statligInkluderingsdugnad == other.statligInkluderingsdugnad

    fun <T> containsExactly(l1: List<T>, l2: List<T>): Boolean = l1.containsAll(l2) && l2.containsAll(l1)
}

@Repository
class InkluderingsmuligheterRepository(val namedJdbcTemplate: NamedParameterJdbcTemplate) {

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
        LOG.info("Lagret inkludering med dbid: $retur og stillingsid: ${inkluderingsmulighet.stillingsid}")
        return retur
    }

    fun hentInkluderingsmulighet(stillingId: String): List<Inkluderingsmulighet> {
        return namedJdbcTemplate.query("SELECT * FROM $inkluderingsmuligheterTabell WHERE $stillingsidFelt = '$stillingId' ORDER BY $idFelt DESC")
        { rs: ResultSet, _: Int -> tilInkludering(rs) }
    }

    fun hentSisteInkluderingsmulighet(stillingId: String): Inkluderingsmulighet? {
        return namedJdbcTemplate.query("SELECT * FROM $inkluderingsmuligheterTabell WHERE $stillingsidFelt = '$stillingId' ORDER BY $idFelt DESC LIMIT 1")
        { rs: ResultSet, _: Int -> tilInkludering(rs) }.firstOrNull()
    }

    private fun tilInkludering(rs: ResultSet): Inkluderingsmulighet {
        return Inkluderingsmulighet(
            stillingsid = rs.getString(stillingsidFelt),
            tilretteleggingmuligheter = tilStringListe(rs.getString(tilretteleggingmuligheterFelt)),
            virkemidler = tilStringListe(rs.getString(virkemidlerFelt)),
            prioriterteMålgrupper = tilStringListe(rs.getString(prioriterteMålgrupperFelt)),
            statligInkluderingsdugnad = rs.getBoolean(statligInkluderingsdugnadFelt),
            radOpprettet = rs.getTimestamp(radOpprettetFelt).toLocalDateTime()
        )
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
