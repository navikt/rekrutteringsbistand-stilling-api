package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.stilling.InternStillingRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class TestRepository(val jdbcTemplate: JdbcTemplate) {

    fun slettAlt() {
        jdbcTemplate.update("DELETE FROM ${StillingsinfoRepository.STILLINGSINFO}")
        jdbcTemplate.update("DELETE FROM ${InternStillingRepository.INTERN_STILLING_TABELL}")

    }
}
