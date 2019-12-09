package no.nav.rekrutteringsbistand.api.autorisasjon

import java.util.*
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtils(private val contextHolder: OIDCRequestContextHolder) {

    companion object {
        const val ISSUER_ISSO = "isso"
    }

    fun hentInnloggetVeileder(): InnloggetVeileder {
        return contextHolder.oidcValidationContext.getClaims(ISSUER_ISSO)
                .run {
                    InnloggetVeileder(
                            userName = get("unique_name"),
                            displayName = get("name"),
                            navIdent = get("NAVident")
                    )
                }
    }

    fun tokenUtløper(): Boolean {
        val hasValidToken = contextHolder.oidcValidationContext.hasValidTokenFor(ISSUER_ISSO)
        val expirationTime = contextHolder.oidcValidationContext.getClaims(ISSUER_ISSO).claimSet.expirationTime
        val inFiveMinutes = Date(System.currentTimeMillis() + 5 * 60000)
        return hasValidToken && inFiveMinutes.after(expirationTime)
    }

    fun hentOidcToken(): String = contextHolder.oidcValidationContext.getToken(ISSUER_ISSO).idToken

    fun harInnloggingsContext(): Boolean {
        return try {
            contextHolder.oidcValidationContext
            true
        } catch (exception: IllegalStateException) { // Kaster exception hvis man prøver å hente context utenfor et request initiert av en bruker
            false
        }
    }
}

data class InnloggetVeileder(
        val userName: String,
        val displayName: String,
        val navIdent: String
)
