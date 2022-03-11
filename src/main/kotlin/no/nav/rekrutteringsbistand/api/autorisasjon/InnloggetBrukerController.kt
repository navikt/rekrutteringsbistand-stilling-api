package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
// @ProtectedWithClaims(issuer = "azuread", claimMap = ["NAVident=*", "name=*"]) TODO: Oppgrader bibliotek
@Protected
class InnloggetBrukerController(private val tokenUtils: TokenUtils) {

    @GetMapping
    fun hentInnloggetVeileder(): InnloggetVeileder {
        return tokenUtils.hentInnloggetVeileder()
    }
}
