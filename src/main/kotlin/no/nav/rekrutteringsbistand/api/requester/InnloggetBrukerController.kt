package no.nav.rekrutteringsbistand.api.requester

import no.nav.security.oidc.api.Protected
import no.nav.security.spring.oidc.SpringOIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
@Protected
class InnloggetBrukerController {

    @Autowired
    @Suppress("SpringJavaInjectionPointsAutowiringInspection") // Denne må aktiveres i spring eller test oicd rammeverk
    lateinit var contextHolder: SpringOIDCRequestContextHolder
    private val ISSUER = "loginservice-oidc"

    @GetMapping
    fun hentInnloggetBruker(): InnloggetBruker =
            contextHolder.oidcValidationContext.getClaims(ISSUER)
                    .run {
                        InnloggetBruker(
                                userName = get("unique_name"),
                                displayName = get("name"),
                                navIdent = get("NAVident"))
                    }

    @GetMapping("/token-expiring")
    fun isExpiringToken(): Boolean =
            contextHolder.oidcValidationContext.hasValidTokenFor(ISSUER) &&
                    Date(System.currentTimeMillis() + 5 * 60000)
                            .after(contextHolder.oidcValidationContext.getClaims(ISSUER).claimSet.expirationTime)

}

data class InnloggetBruker(val userName: String, val displayName: String, val navIdent: String)
