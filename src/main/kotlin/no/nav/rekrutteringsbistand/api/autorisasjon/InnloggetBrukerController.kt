package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils.Companion.ISSUER_ISSO
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
@Protected
class InnloggetBrukerController(private val contextHolder: OIDCRequestContextHolder) {

    @GetMapping
    fun hentInnloggetBruker(): InnloggetBruker =
            contextHolder.oidcValidationContext.getClaims(ISSUER_ISSO)
                    .run {
                        InnloggetBruker(
                                userName = get("unique_name"),
                                displayName = get("name"),
                                navIdent = get("NAVident"))
                    }

    @GetMapping("/token-expiring")
    fun isExpiringToken(): Boolean =
            contextHolder.oidcValidationContext.hasValidTokenFor(ISSUER_ISSO) &&
                    Date(System.currentTimeMillis() + 5 * 60000)
                            .after(contextHolder.oidcValidationContext.getClaims(ISSUER_ISSO).claimSet.expirationTime)

}

data class InnloggetBruker(val userName: String, val displayName: String, val navIdent: String)
