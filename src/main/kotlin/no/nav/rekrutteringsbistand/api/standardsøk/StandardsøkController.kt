package no.nav.rekrutteringsbistand.api.standardsøk

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/standardsok")
@Protected
class StandardsøkController(val standardsøkRepository: StandardsøkRepository, val tokenUtils: TokenUtils) {

    @PutMapping
    fun lagreStandardsøk(@RequestBody lagreStandardsøkDto: LagreStandardsøkDto): ResponseEntity<HentStandardsøkDto> {
        val lagretSøk = standardsøkRepository.oppdaterStandardsøk(lagreStandardsøkDto, tokenUtils.hentInnloggetVeileder().navIdent)
        val returverdi = HentStandardsøkDto(lagretSøk.søk, lagretSøk.navIdent, lagretSøk.tidspunkt)

        return ResponseEntity.status(HttpStatus.CREATED).body(returverdi)
    }
}

data class LagreStandardsøkDto(val søk: String)

data class HentStandardsøkDto(
        val søk: String,
        val navIdent: String,
        val tidspunkt: LocalDateTime
)

data class LagretStandardsøk(
        val id: String,
        val søk: String,
        val navIdent: String,
        val tidspunkt: LocalDateTime
)
