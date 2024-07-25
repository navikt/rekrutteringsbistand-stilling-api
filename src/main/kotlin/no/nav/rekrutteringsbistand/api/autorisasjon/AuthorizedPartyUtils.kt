package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

const val azureAdIssuer = "azuread"

@Component
class AuthorizedPartyUtils(
    private val contextHolder: TokenValidationContextHolder,
) {
    private val authorizedPartyNameClaim = "azp_name"

    @Value("\${rekrutteringsbistand.stilling.indekser.azp-name}")
    private val stillingIndekserAzpName: String = ""

    @Value("\${vis-stilling.azp-name}")
    private val visStillingAzpName: String = ""

    @Value("\${kandidatvarsel.azp-name}")
    private val kandidatvarselAzpName: String = ""

    fun kallKommerFraStillingIndekser(): Boolean {
        return authorizedPartyName() == stillingIndekserAzpName
    }

    fun kallKommerFraVisStilling(): Boolean {
        return authorizedPartyName() == visStillingAzpName
    }

    fun kallKommerFraKandidatvarsel(): Boolean {
        return authorizedPartyName() == kandidatvarselAzpName
    }

    private fun authorizedPartyName(): String? {
        return contextHolder.tokenValidationContext.getClaims(azureAdIssuer).getStringClaim(authorizedPartyNameClaim)
    }
}
