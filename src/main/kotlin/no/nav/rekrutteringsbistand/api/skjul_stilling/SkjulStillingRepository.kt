package no.nav.rekrutteringsbistand.api.skjul_stilling

import no.nav.rekrutteringsbistand.api.stilling.StillingReferanse
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.time.ZoneId
import java.time.ZonedDateTime

class SkjulStillingRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
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

    fun upsertSkjulestatus(skjulestatus: Skjulestatus) =
        namedJdbcTemplate.update(
            """
        insert into skjulestatus (
            stilling_referanse,
            stilling_stanset_tidspunkt,
            utført_markert_for_skjuling,
            utført_slette_elasticsearch,
            utført_skjule_kandidatliste
        ) values (
            :stilling_referanse,
            :stilling_stanset_tidspunkt,
            :utført_markert_for_skjuling,
            :utført_slette_elasticsearch,
            :utført_skjule_kandidatliste
        )
        on conflict (stilling_referanse) do update set
            stilling_stanset_tidspunkt = excluded.stilling_stanset_tidspunkt,
            utført_markert_for_skjuling = excluded.utført_markert_for_skjuling,
            utført_slette_elasticsearch = excluded.utført_slette_elasticsearch,
            utført_skjule_kandidatliste = excluded.utført_skjule_kandidatliste
    """.trimIndent(),
            mapOf(
                "stilling_referanse" to skjulestatus.stillingReferanse.uuid,
                "stilling_stanset_tidspunkt" to skjulestatus.stillingStansetTidspunkt,
                "utført_markert_for_skjuling" to skjulestatus.utførtMarkertForSkjuling,
                "utført_slette_elasticsearch" to skjulestatus.utførtSletteElasticsearch,
                "utført_skjule_kandidatliste" to skjulestatus.utførtSkjuleKandidatliste,
            )
        )
}


private fun ResultSet.getTimestampAsZonedDateTime(columnLabel: String): ZonedDateTime? {
    val timestamp = this.getTimestamp(columnLabel)
    return timestamp?.toInstant()?.atZone(ZoneId.of("Europe/Oslo"))
}