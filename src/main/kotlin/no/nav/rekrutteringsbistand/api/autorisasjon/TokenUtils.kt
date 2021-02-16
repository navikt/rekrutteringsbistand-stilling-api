package no.nav.rekrutteringsbistand.api.autorisasjon

import java.util.*
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtils(private val contextHolder: TokenValidationContextHolder) {

    companion object {
        const val ISSUER_ISSO = "isso"
    }

    fun hentInnloggetVeileder(): InnloggetVeileder {
        return contextHolder.tokenValidationContext.getClaims(ISSUER_ISSO)
                .run {
                    InnloggetVeileder(
                            userName = getStringClaim("unique_name"),
                            displayName = getStringClaim("name"),
                            navIdent = getStringClaim("NAVident")
                    )
                }
    }

    fun tokenUtløper(): Boolean {
        val hasValidToken = contextHolder.tokenValidationContext.hasTokenFor(ISSUER_ISSO)
        val expirationTime = contextHolder.tokenValidationContext.getClaims(ISSUER_ISSO).expirationTime
        val inFiveMinutes = Date(System.currentTimeMillis() + 5 * 60000)
        return hasValidToken && inFiveMinutes.after(expirationTime)
    }

    fun hentOidcToken(): String = contextHolder.tokenValidationContext.getJwtToken(ISSUER_ISSO).tokenAsString

    fun harInnloggingsContext(): Boolean {
        return try {
            contextHolder.tokenValidationContext
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
