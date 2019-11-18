package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TokenUtils @Autowired
constructor(private val contextHolder: OIDCRequestContextHolder) {

    fun hentOidcToken(): String {
        return contextHolder.oidcValidationContext.getToken(ISSUER_ISSO).idToken
    }

    companion object {
        const val ISSUER_ISSO = "isso"
    }

}
