package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
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
        const val PUBLISERT = "publisert"
        const val PUBLISERT_AV_ADMIN = "publisert_av_admin"
        const val ADMIN_STATUS = "admin_status"
        const val UTLØPSDATO = "utlopsdato"
        const val ID = "id"
        const val VERSJON = "versjon"
        const val DIREKTEMELDT_STILLING_TABELL = "direktemeldt_stilling"
    }

    fun lagreDirektemeldtStilling(direktemeldtStilling: DirektemeldtStilling) {
        log.info("Lagrer direktemeldt stilling med uuid: ${direktemeldtStilling.stillingsId}")

        val sql = """
          insert into $DIREKTEMELDT_STILLING_TABELL ($STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS, $PUBLISERT, $PUBLISERT_AV_ADMIN, $ADMIN_STATUS, $UTLØPSDATO, $VERSJON)
            values(:stillingsid, :innhold ::jsonb, :opprettet, :opprettet_av, :sist_endret, :sist_endret_av, :status, :publisert, :publisert_av_admin, :admin_status, :utløpsdato, :versjon)
            on conflict($STILLINGSID) do update 
            set $SIST_ENDRET_AV=:sist_endret_av, 
                $INNHOLD=:innhold ::jsonb,
                $SIST_ENDRET=:sist_endret,
                $STATUS=:status,
                $PUBLISERT=:publisert,
                $PUBLISERT_AV_ADMIN=:publisert_av_admin,
                $ADMIN_STATUS=:admin_status,
                $UTLØPSDATO=:utløpsdato,
                $VERSJON=:versjon + 1
            
            returning id   
        """.trimIndent()

        val params =  mapOf(
            "stillingsid" to direktemeldtStilling.stillingsId,
            "innhold" to objectMapper.writeValueAsString(direktemeldtStilling.innhold),
            "opprettet" to Timestamp.from(direktemeldtStilling.opprettet.toInstant()),
            "opprettet_av" to direktemeldtStilling.opprettetAv,
            "sist_endret" to Timestamp.from(direktemeldtStilling.sistEndret.toInstant()),
            "sist_endret_av" to direktemeldtStilling.sistEndretAv,
            "status" to direktemeldtStilling.status,
            "publisert" to if(direktemeldtStilling.publisert != null) {
                try {
                    Timestamp.from(direktemeldtStilling.publisert.toInstant())
                } catch (e: Exception) {
                    log.warn("Feil ved konvertering av publisert dato til timestamp: ${direktemeldtStilling.publisert} med melding: ${e.message}, stillingsId: ${direktemeldtStilling.stillingsId}")
                    null
                }
            } else {
                null
            },
            "publisert_av_admin" to direktemeldtStilling.publisertAvAdmin,
            "admin_status" to direktemeldtStilling.adminStatus,
            "utløpsdato" to if(direktemeldtStilling.utløpsdato != null) {
                try {
                    Timestamp.from(direktemeldtStilling.utløpsdato.toInstant())
                } catch (e: Exception) {
                    log.warn("Feil ved konvertering av utløpsdato til timestamp: ${direktemeldtStilling.utløpsdato} med melding: ${e.message}, stillingsId: ${direktemeldtStilling.stillingsId}")
                    null
                }
            } else {
                null
            },
            "versjon" to direktemeldtStilling.versjon
        )

        namedJdbcTemplate.query(sql, MapSqlParameterSource(params)) {rs -> if (rs.next()) rs.getLong("id") else null}
    }

     fun hentDirektemeldtStilling(stillingsId: Stillingsid) : DirektemeldtStilling? {
         return hentDirektemeldtStilling(stillingsId.asString())
     }

    fun hentDirektemeldtStilling(stillingsId: String) : DirektemeldtStilling? {
        val sql = "select $ID, $STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS, $PUBLISERT, $PUBLISERT_AV_ADMIN, $ADMIN_STATUS, $UTLØPSDATO, $VERSJON from $DIREKTEMELDT_STILLING_TABELL where $STILLINGSID=:stillingsid"
        val params = mapOf("stillingsid" to UUID.fromString(stillingsId))

        val direktemeldtStilling = namedJdbcTemplate.query(
            sql, params, DirektemeldtStillingRowMapper()
        )

        if (direktemeldtStilling.isEmpty()) {
            log.info("Fant ikke direktemeldt stilling med id: $stillingsId. Det kan være en ekstern stilling.")
            return null
        }

        if (direktemeldtStilling.size > 1) {
            error("Fant mer enn en direktemeldt stilling med id: $stillingsId (antall: ${direktemeldtStilling.size})")
        }
        return direktemeldtStilling[0]
    }

    fun hentAlleDirektemeldteStillinger() : List<DirektemeldtStilling> {
        val sql = "select $STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS, $PUBLISERT, $PUBLISERT_AV_ADMIN, $ADMIN_STATUS, $UTLØPSDATO, $VERSJON from $DIREKTEMELDT_STILLING_TABELL"
        return namedJdbcTemplate.query(sql, DirektemeldtStillingRowMapper()).filterNotNull()
    }

    fun hentStillingerForAktivering() : List<DirektemeldtStilling> {
        // Henter alle stillinger som har status INACTIVE og hvor publisert er i løpet av de siste 24 timer, utløpsdato er fram i tid, admmin_status er DONE og publisert_av_admin er satt
        val sql = """
          select $ID, $STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS, $PUBLISERT, $PUBLISERT_AV_ADMIN, $ADMIN_STATUS, $UTLØPSDATO, $VERSJON
          from
              $DIREKTEMELDT_STILLING_TABELL
          where
              $STATUS = 'INACTIVE' 
              and $PUBLISERT > now() - interval '1 day'
              and $PUBLISERT <= now()
            and (
            $UTLØPSDATO IS NULL
            or $UTLØPSDATO >= DATE_TRUNC('day', CURRENT_TIMESTAMP)
            )
            and $PUBLISERT_AV_ADMIN is not null
            and $ADMIN_STATUS = 'DONE';
        """.trimIndent()

        return namedJdbcTemplate.query(
            sql, DirektemeldtStillingRowMapper()
        ).filterNotNull()
    }

    fun hentStillingerForDeaktivering(): List<DirektemeldtStilling> {
        // Henter alle direktemeldte stillinger som har status ACTIVE og hvor utløpsdato er før dagens dato
        val sql = """
            select $ID, $STILLINGSID, $INNHOLD, $OPPRETTET, $OPPRETTET_AV, $SIST_ENDRET, $SIST_ENDRET_AV, $STATUS, $PUBLISERT, $PUBLISERT_AV_ADMIN, $ADMIN_STATUS, $UTLØPSDATO, $VERSJON
            from
                $DIREKTEMELDT_STILLING_TABELL
            where
                $STATUS = 'ACTIVE'
                and $UTLØPSDATO < DATE_TRUNC('day', CURRENT_TIMESTAMP)
        """.trimIndent()

        return namedJdbcTemplate.query(
            sql, DirektemeldtStillingRowMapper()
        ).filterNotNull()
    }

    class DirektemeldtStillingRowMapper : RowMapper<DirektemeldtStilling?> {
        @Throws(SQLException::class)
        override fun mapRow(rs: ResultSet, rowNum: Int): DirektemeldtStilling {
            val direktemeldtStilling = DirektemeldtStilling(
                stillingsId = rs.getObject("stillingsid", UUID::class.java),
                innhold = objectMapper.readValue(rs.getString("innhold"), DirektemeldtStillingInnhold::class.java),
                opprettet = rs.getTimestamp("opprettet").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = rs.getString("opprettet_av"),
                sistEndret = rs.getTimestamp("sist_endret").toInstant().atZone(ZoneId.of("Europe/Oslo")),
                sistEndretAv = rs.getString("sist_endret_av"),
                status = rs.getString("status"),
                annonseId = rs.getLong("id"),
                versjon = rs.getInt("versjon"),
                utløpsdato = rs.getTimestamp("utlopsdato")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
                publisert = rs.getTimestamp("publisert")?.toInstant()?.atZone(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = rs.getString("publisert_av_admin"),
                adminStatus = rs.getString("admin_status"),
            )

            return direktemeldtStilling
        }
    }
}
