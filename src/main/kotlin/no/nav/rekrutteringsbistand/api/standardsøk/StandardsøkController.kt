package no.nav.rekrutteringsbistand.api.standardsøk

import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class StandardsøkController {

}

data class LagreStandardsøkDto(val søk: String)

data class HentStandardsøkDto(
        val søk: String,
        val navIdent: String,
        val tidspunkt: LocalDateTime
)

data class LagretStandardsøk(
        val id: Int,
        val søk: String,
        val navIdent: String,
        val tidspunkt: LocalDateTime
)
