package no.nav.rekrutteringsbistand.api

import com.nimbusds.jwt.JWTClaimsSet
import no.nav.security.oidc.context.OIDCRequestContextHolder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*

@Component
class TokenUtils @Autowired
constructor(private val contextHolder: OIDCRequestContextHolder) {

    fun hentOidcToken(): String {
        return contextHolder.oidcValidationContext.getToken(ISSUER_ISSO).idToken
    }

    private fun hentClaimSet(issuer: String): Optional<JWTClaimsSet> {
        return Optional.ofNullable(contextHolder.oidcValidationContext.getClaims(issuer))
                .map({ it.getClaimSet() })
    }

    private fun erNAVIdent(str: String?): Boolean {
        return str != null && str.matches("^[A-Z][0-9]{6}".toRegex())
    }

    companion object {
        val ISSUER_ISSO = "isso"
    }

}
