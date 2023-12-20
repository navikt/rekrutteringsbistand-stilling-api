package no.nav.rekrutteringsbistand.api.skjul_stilling

import no.nav.rekrutteringsbistand.api.stilling.StillingReferanse
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.time.ZoneId
import java.time.ZonedDateTime

class SkjulStillingRepository(
    private val  namedJdbcTemplate: NamedParameterJdbcTemplate,
) {

    data class Skjulestatus(
        val stillingReferanse: StillingReferanse,
        val stillingStansetTidspunkt: ZonedDateTime?,
        val utførtMarkertForSkjuling: ZonedDateTime?,
        val utførtSletteElasticsearch: ZonedDateTime?,
        val utførtSkjuleKandidatliste: ZonedDateTime?,
    )



    fun lookupSkjulestatus(stillingReferanse: StillingReferanse): Skjulestatus? {
        return namedJdbcTemplate.query(
            """
                select *
                from skjulestatus
                where stilling_referanse = :stilling_referanse
            """.trimIndent(),
            mapOf(
                "stillingsid" to stillingReferanse.uuid
            )
        ) { it, _ ->
            Skjulestatus(
                stillingReferanse = StillingReferanse(it.getString("stilling_referanse")),
                stillingStansetTidspunkt = it.getTimestampAsZonedDateTime("stilling_stanset_tidspunkt"),
                utførtMarkertForSkjuling = it.getTimestampAsZonedDateTime("utført_markert_for_skjuling"),
                utførtSletteElasticsearch = it.getTimestampAsZonedDateTime("utført_slette_elasticsearch"),
                utførtSkjuleKandidatliste = it.getTimestampAsZonedDateTime("utført_skjule_kandidatliste"),
            )
        }
            .firstOrNull()
    }
}


private fun ResultSet.getTimestampAsZonedDateTime(columnLabel: String): ZonedDateTime? {
    val timestamp = this.getTimestamp(columnLabel)
    return timestamp?.toInstant()?.atZone(ZoneId.of("Europe/Oslo"))
}