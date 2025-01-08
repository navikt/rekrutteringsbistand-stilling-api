package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.support.secureLog
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException

@Component
class TokenUtils(
    private val contextHolder: TokenValidationContextHolder,
    private val azureKlient: AzureKlient,
    @Value("\${ad.groups.jobbsøkerrettet}") private val adGroupJobbsøkerrettet: String,
    @Value("\${ad.groups.arbeidsgiverrettet}") private val adGroupArbeidsgiverrettet: String,
    @Value("\${ad.groups.utvikler}") private val adGroupUtvikler: String,
) {
    companion object {
        const val ISSUER_AZUREAD = "azuread"
    }

    fun hentInnloggetVeileder(): InnloggetVeileder {
        val claims = contextHolder.getTokenValidationContext().getClaims(ISSUER_AZUREAD)

        return claims.run {
            InnloggetVeileder(
                displayName = getStringClaim("name"),
                navIdent = getStringClaim("NAVident"),
                roller = getAsList("groups").mapNotNull { it.tilRolle() }
            )
        }
    }

    private fun String.tilRolle() = when(this) {
        adGroupJobbsøkerrettet -> Rolle.JOBBSØKERRETTET
        adGroupArbeidsgiverrettet -> Rolle.ARBEIDSGIVERRETTET
        adGroupUtvikler -> Rolle.UTVIKLER
        else -> null
    }

    fun hentNavIdent(): String {
        val claims = contextHolder.getTokenValidationContext().getClaims(ISSUER_AZUREAD)
        return claims.getStringClaim("NAVident")
    }

    fun hentToken(): String {
        val token = contextHolder.getTokenValidationContext().getJwtToken(ISSUER_AZUREAD) ?: throw RuntimeException("Fant ikke token")
        return token.tokenAsString
    }

    fun hentOBOToken(scope: String): String = azureKlient.hentOBOToken(scope, hentNavIdent(), hentToken())
    fun hentSystemToken(scope: String): String = azureKlient.hentSystemToken(scope)
}

enum class Rolle {
    JOBBSØKERRETTET,
    ARBEIDSGIVERRETTET,
    UTVIKLER
}

data class InnloggetVeileder(
    val displayName: String,
    val navIdent: String,
    val roller: List<Rolle>
) {
    fun validerMinstEnAvRollene(vararg potensielleRoller: Rolle) {
        if ((potensielleRoller.toList() + Rolle.UTVIKLER).any { it in roller }) {
            return
        }
        secureLog.info("403 $navIdent med roller $roller har ikke tilgang til funksjon som krever $potensielleRoller ${hentStackTrace()}")
        throw HttpClientErrorException.create("Har roller $roller, trenger minst en av ${potensielleRoller.toList()}", HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders(),byteArrayOf(),null)
    }
}

private fun hentStackTrace() = Thread.currentThread()
    .stackTrace
    .drop(2)
    .filter { it.className.startsWith("no.nav") }
    .joinToString("\n") { element ->
        "${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})"
    }