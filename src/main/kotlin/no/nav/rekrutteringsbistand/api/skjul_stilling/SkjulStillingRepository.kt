package no.nav.rekrutteringsbistand.api.skjul_stilling

import no.nav.rekrutteringsbistand.api.stilling.StillingReferanse
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Repository
class SkjulStillingRepository(
    private val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {

    data class Skjulestatus(
        val stillingReferanse: Stillingsid,
        val grunnlagForSkjuling: LocalDate?,
        val utførtMarkereForSkjuling: ZonedDateTime?,
        val utførtSletteElasticsearch: ZonedDateTime?,
        val utførtSkjuleKandidatliste: ZonedDateTime?,
    )


    fun hentSkjulestatus(stillingsid: Stillingsid): Skjulestatus? {
        return namedJdbcTemplate.query(
            """
                select *
                from skjulestatus
                where stilling_referanse = :stilling_referanse
            """.trimIndent(),
            mapOf(
                "stillingsid" to stillingsid.asString()
            )
        ) { it, _ ->
            Skjulestatus(
                stillingReferanse = Stillingsid(it.getString("stilling_referanse")),
                grunnlagForSkjuling = it.getDate("grunnlag_for_skjuling").toLocalDate(),
                utførtMarkereForSkjuling = it.getTimestampAsZonedDateTime("utført_markere_for_skjuling"),
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
            grunnlag_for_skjuling,
            utført_markere_for_skjuling,
            utført_slette_elasticsearch,
            utført_skjule_kandidatliste
        ) values (
            :stilling_referanse,
            :grunnlag_for_skjuling,
            :utført_markere_for_skjuling,
            :utført_slette_elasticsearch,
            :utført_skjule_kandidatliste
        )
        on conflict (stilling_referanse) do update set
            grunnlag_for_skjuling = excluded.grunnlag_for_skjuling,
            utført_markere_for_skjuling = excluded.utført_markere_for_skjuling,
            utført_slette_elasticsearch = excluded.utført_slette_elasticsearch,
            utført_skjule_kandidatliste = excluded.utført_skjule_kandidatliste
    """.trimIndent(),
            mapOf(
                "stilling_referanse" to skjulestatus.stillingReferanse.asString(),
                "grunnlag_for_skjuling" to java.sql.Date.valueOf(skjulestatus.grunnlagForSkjuling),
                "utført_markere_for_skjuling" to skjulestatus.utførtMarkereForSkjuling,
                "utført_slette_elasticsearch" to skjulestatus.utførtSletteElasticsearch,
                "utført_skjule_kandidatliste" to skjulestatus.utførtSkjuleKandidatliste,
            )
        )
}


private fun ResultSet.getTimestampAsZonedDateTime(columnLabel: String): ZonedDateTime? {
    val timestamp = this.getTimestamp(columnLabel)
    return timestamp?.toInstant()?.atZone(ZoneId.of("Europe/Oslo"))
}