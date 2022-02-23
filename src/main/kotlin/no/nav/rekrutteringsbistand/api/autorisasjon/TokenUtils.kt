package no.nav.rekrutteringsbistand.api.autorisasjon

import java.util.*
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtils(private val contextHolder: TokenValidationContextHolder) {

    companion object {
        const val ISSUER_ISSO = "isso"
        const val ISSUER_AZUREAD = "azuread"
    }

    fun hentInnloggetVeileder(): InnloggetVeileder {
        val claimsFromIssoIdToken = contextHolder.tokenValidationContext.getClaims(ISSUER_ISSO)
        val claimsFromAzureAdToken = contextHolder.tokenValidationContext.getClaims(ISSUER_AZUREAD)

        val validClaims = claimsFromIssoIdToken ?: claimsFromAzureAdToken;
        return validClaims.run {
            InnloggetVeileder(
                displayName = getStringClaim("name"),
                navIdent = getStringClaim("NAVident")
            )
        }
    }

    fun hentNavIdent(): String {
        val claimsFromIssoIdToken = contextHolder.tokenValidationContext.getClaims(ISSUER_ISSO)
        val claimsFromAzureAdToken = contextHolder.tokenValidationContext.getClaims(ISSUER_AZUREAD)

        val validClaims = claimsFromIssoIdToken ?: claimsFromAzureAdToken
        return validClaims.getStringClaim("NAVident")
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
        val displayName: String,
        val navIdent: String
)
