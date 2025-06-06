package no.nav.rekrutteringsbistand.api.stilling.outbox

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class StillingOutboxRepository(private val namedJdbcTemplate: NamedParameterJdbcTemplate) {

    fun lagreMeldingIOutbox(stillingsId: UUID, eventName: EventName) {
        val sql = """
            insert into stilling_outbox (stillingsid, event_name, opprettet)
            values(:stillingsId, :eventName, now())
        """.trimIndent()

        val params = mapOf(
            "stillingsId" to stillingsId,
            "eventName" to eventName.toString()
        )

        namedJdbcTemplate.update(sql, MapSqlParameterSource(params))
    }

    fun finnBatchMedUprossesertMeldinger(): List<StillingOutboxMelding> {
        val sql = """
            select id, stillingsid, event_name
            from stilling_outbox
            where prosessert is null
            order by case when event_name = 'indekserDirektemeldtStilling' then 1 else 2 end, opprettet
            limit 1000
        """.trimIndent()

        return namedJdbcTemplate.query(sql) { rs, _ ->
            StillingOutboxMelding(
                id = rs.getLong("id"),
                stillingsId = UUID.fromString(rs.getString("stillingsid")),
                eventName = EventName.fromString(rs.getString("event_name"))
            )
        }
    }

    fun settSomProsessert(id: Long) {
        val sql = """
            update stilling_outbox
            set prosessert = now()
            where id = :id
        """.trimIndent()

        namedJdbcTemplate.update(sql, MapSqlParameterSource("id", id))
    }
}
