package no.nav.rekrutteringsbistand.api.minestillinger

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class MineStillingerRepository(
    val namedJdbcTemplate: NamedParameterJdbcTemplate,
) {
// TODO

    fun hent(navIdent: String): List<MinStilling> {
        return listOf()
    }

}
