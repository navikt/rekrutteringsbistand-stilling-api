package no.nav.rekrutteringsbistand.api.innloggetbruker

import no.nav.security.oidc.api.ProtectedWithClaims
import no.nav.security.spring.oidc.SpringOIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/reportee")
@ProtectedWithClaims(issuer = "loginservice-oidc")
class InnloggetBrukerController {

    @Autowired
    lateinit var contextHolder: SpringOIDCRequestContextHolder
    private val ISSUER = "loginservice-oidc"

    @GetMapping
    fun hentInnloggetBruker(): InnloggetBruker {
        val claims = contextHolder.oidcValidationContext.getClaims(ISSUER)
        return InnloggetBruker(
                userName = claims.get("unique_name"),
                displayName = claims.get("name"),
                navIdent = claims.get("NAVident"))
    }

    @GetMapping("/token-expiring")
    fun isExpiringToken(): Boolean {
        if (contextHolder.oidcValidationContext.hasValidTokenFor(ISSUER)) {
            val claims = contextHolder.oidcValidationContext.getClaims(ISSUER)
            val expirationTime = claims.claimSet.expirationTime
            val plusFiveMinutes = Date(System.currentTimeMillis() + 5 * 60000)
            return plusFiveMinutes.after(expirationTime)
        }
        return false
    }

}

data class InnloggetBruker (val userName: String, val displayName: String, val navIdent: String)
