package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository


@Repository
class InkluderingRepository(
        val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun lagreInkluderingBatch(inkluderingsmuligheter: List<Inkluderingsmuligheter>) {
        LOG.info("Kaller lagreInkludering")
        val batchupdate = namedJdbcTemplate.batchUpdate(
                """INSERT INTO INKLUDERINGSMULIGHETER($stillingsid, $tilretteleggingmuligheter, $virkemidler, $prioriterte_maalgrupper, $statlig_inkluderingsdugnad, $rad_opprettet) 
                    |VALUES(:$stillingsid, :$tilretteleggingmuligheter, :$virkemidler, :$prioriterte_maalgrupper, :$statlig_inkluderingsdugnad, $rad_opprettet)
                """.trimMargin(),
                inkluderingsmuligheter.map {
                    MapSqlParameterSource(
                            mapOf(
                                    stillingsid to it.stillingsid,
                                    tilretteleggingmuligheter to it.tilretteleggingmuligheter,
                                    virkemidler to it.virkemidler,
                                    prioriterte_maalgrupper to it.prioriterte_maalgrupper,
                                    statlig_inkluderingsdugnad to it.statlig_inkluderingsdugnad
                            )
                    )
                }.toTypedArray()
        )


    }

    companion object {
        const val id = "id"
        const val stillingsid = "stillingsid"
        const val tilretteleggingmuligheter = "tilretteleggingmuligheter"
        const val virkemidler = "virkemidler"
        const val prioriterte_maalgrupper = "prioriterte_maalgrupper"
        const val statlig_inkluderingsdugnad = "statlig_inkluderingsdugnad"
        const val rad_opprettet = "rad_opprettet"
    }
}
