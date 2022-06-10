package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val azureAdIssuer = "azuread"

@Component
class AuthorizedPartyUtils(
        private val contextHolder: TokenValidationContextHolder,
) {
    private val authorizedPartyUriClaim = "azp_name"

    @Value("\${rekrutteringsbistand.stilling.indekser.uri}")
    private val stillingIndekserUri: String = ""

    @Value("\${vis-stilling.uri}")
    private val visStillingUri: String = ""

    fun kallKommerFraStillingIndekser(): Boolean {
        return uriTilKallendeApp() == stillingIndekserUri
    }

    fun kallKommerFraVisStilling(): Boolean {
        return uriTilKallendeApp() == visStillingUri
    }

    private fun uriTilKallendeApp(): String {
        return contextHolder.tokenValidationContext.getClaims(azureAdIssuer).getStringClaim(authorizedPartyUriClaim)
    }
}
