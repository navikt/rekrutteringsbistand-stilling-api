package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.ZoneId
import java.util.*


@Repository
class InternStillingRepository(private val namedJdbcTemplate: NamedParameterJdbcTemplate) {

    companion object {
        val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

        const val STILLINGSID = "stillingsid"
        const val INNHOLD = "innhold"
        const val OPPRETTET = "opprettet"
        const val OPPRETTET_AV = "opprettet_av"
        const val SIST_ENDRET = "sist_endret"
        const val SIST_ENDRET_AV = "sist_endret_av"
        const val INTERN_STILLING_TABELL = "intern_stilling"
    }

    fun lagreInternStilling(internStilling: InternStilling) {
        log.info("Lagrer intern stilling med uuid: ${internStilling.stillingsid}")

        val sql = """
          insert into intern_stilling ($STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV)
            values(:stillingsid, :innhold ::jsonb, :opprettet, :opprettet_av, :sist_endret, :sist_endret_av)
            on conflict($STILLINGSID) do update 
            set $SIST_ENDRET_AV=:sist_endret_av, 
                $INNHOLD=:innhold ::jsonb,
                $SIST_ENDRET=:sist_endret
            returning id   
        """.trimIndent()

        val params =  mapOf(
            "stillingsid" to internStilling.stillingsid,
            "innhold" to objectMapper.writeValueAsString(internStilling.innhold),
            "opprettet" to Timestamp.from(internStilling.opprettet.toInstant()),
            "opprettet_av" to internStilling.opprettetAv,
            "sist_endret" to Timestamp.from(internStilling.sistEndret.toInstant()),
            "sist_endret_av" to internStilling.sistEndretAv
        )

       namedJdbcTemplate.query(sql, MapSqlParameterSource(params)) {rs -> if (rs.next()) rs.getLong("id") else null}
    }

    fun getInternStilling(stillingsid: String) : InternStilling {
        val sql = "select stillingsid, innhold, opprettet, opprettet_av, sist_endret, sist_endret_av from intern_stilling where $STILLINGSID=:stillingsid ::uuid"
        val params = mapOf("stillingsid" to stillingsid)

        val internStilling = namedJdbcTemplate.queryForObject(
            sql, params, InternStillingRowMapper()
        )

        if(internStilling == null) {
            throw RuntimeException("Fant ikke intern stilling")
        }

        return internStilling
    }


    class InternStillingRowMapper : RowMapper<InternStilling?> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): InternStilling {
            val internStilling = InternStilling(
                stillingsid = rs.getObject("stillingsid", UUID::class.java),
                innhold = objectMapper.readValue(rs.getString("innhold"), Stilling::class.java),
                opprettet = rs.getTimestamp("opprettet").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = rs.getString("opprettet_av"),
                sistEndret = rs.getTimestamp("sist_endret").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                sistEndretAv = rs.getString("sist_endret_av")
            )

            return internStilling
        }
    }
}
