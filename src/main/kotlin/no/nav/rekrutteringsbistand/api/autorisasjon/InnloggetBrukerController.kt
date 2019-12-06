package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.oidc.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
class InnloggetBrukerController(private val tokenUtils: TokenUtils) {

    @GetMapping
    fun hentInnloggetVeileder(): InnloggetVeileder = tokenUtils.hentInnloggetVeileder()

    @GetMapping("/token-expiring")
    fun tokenUtløper(): Boolean = tokenUtils.tokenUtløper()
}
