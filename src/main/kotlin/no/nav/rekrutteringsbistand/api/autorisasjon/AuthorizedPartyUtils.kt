package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val azureAdIssuer = "azuread"

@Component
class AuthorizedPartyUtils(
        private val contextHolder: TokenValidationContextHolder,
) {
    private val authorizedPartyClaim = "azp"

    @Value("\${rekrutteringsbistand.stilling.indekser.client.id}")
    private val clientIdTilStillingIndekser: String = ""

    @Value("\${vis-stilling.client.id}")
    private val clientIdTilVisStilling: String = ""

    fun kallKommerFraStillingIndekser(): Boolean {
        return clientIdTilKallendeApp() == clientIdTilStillingIndekser
    }

    fun kallKommerFraVisStilling(): Boolean {
        return clientIdTilKallendeApp() == clientIdTilVisStilling
    }

    private fun clientIdTilKallendeApp(): String {
        return contextHolder.tokenValidationContext.getClaims(azureAdIssuer).getStringClaim(authorizedPartyClaim)
    }
}
