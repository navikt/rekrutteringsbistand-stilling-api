package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class InkluderingRepository (
        val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {

    fun lagreInkludering(inkludering: String) : Unit {
        LOG.info("lagrer $inkludering")
    }

}