package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.secureLog

const val azureAdIssuer = "azuread"

@Component
class AuthorizedPartyUtils(
    private val contextHolder: TokenValidationContextHolder,
) {
    private val authorizedPartyNameClaim = "azp_name"

    @Value("\${vis-stilling.azp-name}")
    private val visStillingAzpName: String = ""

    @Value("\${kandidatvarsel.azp-name}")
    private val kandidatvarselAzpName: String = ""

    @Value("\${toi.stilling.indekser.azp-name}")
    private val toiStillingIndekserAzpName: String = ""

    fun kallKommerFraStillingIndekser(): Boolean {
        return authorizedPartyName() == toiStillingIndekserAzpName
    }

    fun kallKommerFraVisStilling(): Boolean {
        return authorizedPartyName() == visStillingAzpName
    }

    fun kallKommerFraKandidatvarsel(): Boolean {
        return authorizedPartyName() == kandidatvarselAzpName
    }

    private fun authorizedPartyName(): String? {
        return contextHolder.getTokenValidationContext().getClaims(azureAdIssuer).getStringClaim(authorizedPartyNameClaim)
    }
}
