package no.nav.rekrutteringsbistand.api.support.featuretoggle

import no.finn.unleash.Unleash
import no.finn.unleash.UnleashContext
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import org.springframework.stereotype.Service

@Service
class FeatureToggleService(
        val unleash: Unleash,
        val tokenUtils: TokenUtils
) {

    fun isEnabled(feature: String): Boolean {
        return unleash.isEnabled(feature, contextMedInnloggetBruker())
    }

    private fun contextMedInnloggetBruker(): UnleashContext {
        val builder = UnleashContext.builder()
        if (tokenUtils.harInnloggingsContext()) {
            val veileder: InnloggetVeileder = tokenUtils.hentInnloggetVeileder()
            builder.userId(veileder.navIdent)
        }
        return builder.build()
    }
}
