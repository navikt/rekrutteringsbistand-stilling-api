package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository



@Repository
class InkluderingRepository (
        val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
    val simpleJdbcInsert = SimpleJdbcInsert(namedJdbcTemplate.jdbcTemplate)
        .withTableName("inkluderingsmuligheter")
        .usingGeneratedKeyColumns("id")

    fun lagreInkluderingBatch(stillinger: List<Inkluderingsmuligheter>) {
        LOG.info("Kaller lagreInkludering")
        simpleJdbcInsert.executeAndReturnKey(
            mapOf(
                stillingsid to
            )
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
