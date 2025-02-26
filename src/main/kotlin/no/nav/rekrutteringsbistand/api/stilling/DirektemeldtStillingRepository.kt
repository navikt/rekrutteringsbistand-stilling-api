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
class DirektemeldtStillingRepository(private val namedJdbcTemplate: NamedParameterJdbcTemplate) {
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
        const val STATUS = "status"
        const val DIREKTEMELDT_STILLING_TABELL = "direktemeldt_stilling"
    }

    fun lagreDirektemeldtStilling(direktemeldtStilling: DirektemeldtStilling) {
        log.info("Lagrer direktemeldt stilling med uuid: ${direktemeldtStilling.stillingsid}")

        val sql = """
          insert into $DIREKTEMELDT_STILLING_TABELL ($STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS)
            values(:stillingsid, :innhold ::jsonb, :opprettet, :opprettet_av, :sist_endret, :sist_endret_av, :status)
            on conflict($STILLINGSID) do update 
            set $SIST_ENDRET_AV=:sist_endret_av, 
                $INNHOLD=:innhold ::jsonb,
                $SIST_ENDRET=:sist_endret,
                $STATUS=:status
            returning id   
        """.trimIndent()

        val params =  mapOf(
            "stillingsid" to direktemeldtStilling.stillingsid,
            "innhold" to objectMapper.writeValueAsString(direktemeldtStilling.innhold),
            "opprettet" to Timestamp.from(direktemeldtStilling.opprettet.toInstant()),
            "opprettet_av" to direktemeldtStilling.opprettetAv,
            "sist_endret" to Timestamp.from(direktemeldtStilling.sistEndret.toInstant()),
            "sist_endret_av" to direktemeldtStilling.sistEndretAv,
            "status" to direktemeldtStilling.status
        )

        namedJdbcTemplate.query(sql, MapSqlParameterSource(params)) {rs -> if (rs.next()) rs.getLong("id") else null}
    }

    fun hentDirektemeldtStilling(stillingsid: String) : DirektemeldtStilling {
        val sql = "select stillingsid, innhold, opprettet, opprettet_av, sist_endret, sist_endret_av, status from $DIREKTEMELDT_STILLING_TABELL where $STILLINGSID=:stillingsid ::uuid"
        val params = mapOf("stillingsid" to stillingsid)

        val direktemeldtStilling = namedJdbcTemplate.queryForObject(
            sql, params, DirektemeldtStillingRowMapper()
        )

        if(direktemeldtStilling == null) {
            throw RuntimeException("Fant ikke direktemeldt stilling")
        }

        return direktemeldtStilling
    }

    fun hentStillingerForAktivering() : List<DirektemeldtStilling> {
        // Henter alle stillinger som har status INACTIVE og hvor published er i løpet av de siste 24 timer, expires er fram i tid, admminstatus er DONE og publishedByAdmin er satt
        val sql = """
          select stillingsid, innhold, opprettet, opprettet_av, sist_endret, sist_endret_av, status
          from
              $DIREKTEMELDT_STILLING_TABELL
          where
              status = 'INACTIVE' 
              and (innhold->>'published')::timestamp > now() - interval '1 day'
              and (innhold->>'published')::timestamp <= now()
            and (
            (innhold->>'expires')::timestamp IS NULL
            or (innhold->>'expires')::timestamp >= DATE_TRUNC('day', CURRENT_TIMESTAMP)
            )
            and (innhold->>'publishedByAdmin') is not null
            and innhold->'administration'->>'status' = 'DONE';
        """.trimIndent()

        return namedJdbcTemplate.query(
            sql, DirektemeldtStillingRowMapper()
        ).filterNotNull()
    }

    fun hentStillingerForDeaktivering(): List<DirektemeldtStilling> {
        // Henter alle direktemeldte stillinger som har status ACTIVE og hvor expires er før dagens dato
        val sql = """
            select stillingsid, innhold, opprettet, opprettet_av, sist_endret, sist_endret_av, status
            from
                $DIREKTEMELDT_STILLING_TABELL
            where
                status = 'ACTIVE'
                and (innhold->>'expires')::timestamp < DATE_TRUNC('day', CURRENT_TIMESTAMP)
        """.trimIndent()

        return namedJdbcTemplate.query(
            sql, DirektemeldtStillingRowMapper()
        ).filterNotNull()
    }

    class DirektemeldtStillingRowMapper : RowMapper<DirektemeldtStilling?> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): DirektemeldtStilling {
            val direktemeldtStilling = DirektemeldtStilling(
                stillingsid = rs.getObject("stillingsid", UUID::class.java),
                innhold = objectMapper.readValue(rs.getString("innhold"), DirektemeldtStillingBlob::class.java),
                opprettet = rs.getTimestamp("opprettet").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = rs.getString("opprettet_av"),
                sistEndret = rs.getTimestamp("sist_endret").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                sistEndretAv = rs.getString("sist_endret_av"),
                status = rs.getString("status"),
            )

            return direktemeldtStilling
        }
    }
}
