package no.nav.rekrutteringsbistand.api.standardsøk

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.ResultSet
import java.time.LocalDateTime

@RestController
@RequestMapping("/standardsok")
@ProtectedWithClaims(issuer = "azuread")
class StandardsøkController(val standardsøkService: StandardsøkService, val tokenUtils: TokenUtils) {

    @PutMapping
    fun opprettEllerOppdaterStandardsøk(@RequestBody lagreStandardsøkDto: LagreStandardsøkDto): ResponseEntity<Any> {
        val lagretSøk = standardsøkService.oppdaterStandardsøk(lagreStandardsøkDto, tokenUtils.hentInnloggetVeileder().navIdent)
        if (lagretSøk != null) {
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(HentStandardsøkDto(lagretSøk.søk, lagretSøk.navIdent, lagretSøk.tidspunkt))
        }

        return ResponseEntity
                .status(500)
                .body("Kunne ikke lagre standardsøk")
    }

    @GetMapping
    fun hentStandardsøk(): ResponseEntity<Any> {
        val navIdent = tokenUtils.hentInnloggetVeileder().navIdent
        val lagretSøk = standardsøkService.hentStandardsøk(navIdent)
        if (lagretSøk != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(HentStandardsøkDto(lagretSøk.søk, lagretSøk.navIdent, lagretSøk.tidspunkt))
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Fant ikke standardsøk på gitt ident $navIdent")
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
) {
    companion object {
        fun fromDB(rs: ResultSet) =
                LagretStandardsøk(
                        id = rs.getString("id"),
                        søk = rs.getString("sok"),
                        navIdent = rs.getString("nav_ident"),
                        tidspunkt = rs.getTimestamp("tidspunkt").toLocalDateTime()
                )
    }
}
