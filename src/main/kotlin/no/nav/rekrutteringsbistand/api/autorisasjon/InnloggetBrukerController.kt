package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
@RequiredIssuers(
    value = [
        ProtectedWithClaims(issuer = "isso"),
        ProtectedWithClaims(issuer = "azuread")
    ]
)
class InnloggetBrukerController(private val tokenUtils: TokenUtils) {

    @GetMapping
    fun hentInnloggetVeileder(): InnloggetVeileder {
        return tokenUtils.hentInnloggetVeileder()
    }
}
