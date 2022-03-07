package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtils(
    private val contextHolder: TokenValidationContextHolder,
    private val azureKlient: AzureKlient,
) {
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

    fun hentToken(): String {
        val tokenFromIssoIdToken = contextHolder.tokenValidationContext.getJwtToken(ISSUER_ISSO)
        val tokenFromAzureAdToken = contextHolder.tokenValidationContext.getJwtToken(ISSUER_AZUREAD)

        val validToken = tokenFromIssoIdToken ?: tokenFromAzureAdToken
        return validToken.tokenAsString
    }

    fun brukerIssoIdToken(): Boolean {
        val tokenFromIssoIdToken = contextHolder.tokenValidationContext.getJwtToken(ISSUER_ISSO)
        return tokenFromIssoIdToken !== null;
    }

    fun harInnloggingsContext(): Boolean {
        return try {
            contextHolder.tokenValidationContext
            true
        } catch (exception: IllegalStateException) { // Kaster exception hvis man prøver å hente context utenfor et request initiert av en bruker
            false
        }
    }

    fun hentOBOToken(scope: String): String = azureKlient.hentOBOToken(scope, hentNavIdent(), hentToken())
    fun hentSystemToken(scope: String): String = azureKlient.hentSystemToken(scope)
}

data class InnloggetVeileder(
        val displayName: String,
        val navIdent: String
)
