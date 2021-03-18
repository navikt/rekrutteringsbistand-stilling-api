package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.stereotype.Component

@Component
class AuthorizedPartyUtils(private val contextHolder: TokenValidationContextHolder) {

    val issuer = "azuread"
    val authorizedPartyClaim = "azp"

    val clientIdTilStillingIndekser: String = System.getenv("REKRUTTERINGSBISTAND_STILLING_INDEKSER_CLIENT_ID")
    val clientIdTilVisStilling: String = System.getenv("VIS_STILLING_CLIENT_ID")

    fun kallKommerFraStillingIndekser(): Boolean {
        return clientIdTilKallendeApp() == clientIdTilStillingIndekser
    }

    fun kallKommerFraVisStilling(): Boolean {
        return clientIdTilKallendeApp() == clientIdTilVisStilling
    }

    private fun clientIdTilKallendeApp(): String {
        return contextHolder.tokenValidationContext.getClaims(issuer).getStringClaim(authorizedPartyClaim)
    }
}
