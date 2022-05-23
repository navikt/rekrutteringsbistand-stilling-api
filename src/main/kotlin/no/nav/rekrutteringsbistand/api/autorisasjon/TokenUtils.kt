package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class TokenUtils(
    private val contextHolder: TokenValidationContextHolder,
    private val azureKlient: AzureKlient,
) {
    companion object {
        const val ISSUER_AZUREAD = "azuread"
    }

    fun hentInnloggetVeileder(): InnloggetVeileder {
        val claims = contextHolder.tokenValidationContext.getClaims(ISSUER_AZUREAD)

        return claims.run {
            InnloggetVeileder(
                displayName = getStringClaim("name"),
                navIdent = getStringClaim("NAVident")
            )
        }
    }

    fun hentNavIdent(): String {
        val claims = contextHolder.tokenValidationContext.getClaims(ISSUER_AZUREAD)
        return claims.getStringClaim("NAVident")
    }

    fun hentToken(): String {
        val token = contextHolder.tokenValidationContext.getJwtToken(ISSUER_AZUREAD)
        return token.tokenAsString
    }

    fun hentOBOToken(scope: String): String = azureKlient.hentOBOToken(scope, hentNavIdent(), hentToken())
    fun hentSystemToken(scope: String): String = azureKlient.hentSystemToken(scope)
}

data class InnloggetVeileder(
        val displayName: String,
        val navIdent: String
)
