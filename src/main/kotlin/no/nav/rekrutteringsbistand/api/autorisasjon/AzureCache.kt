package no.nav.rekrutteringsbistand.api.autorisasjon

import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class AzureCache {
    private val cache: HashMap<String, HashMap<String, CachedToken>> = HashMap()

    fun hentOBOToken(scope: String, onBehalfOf: String): String? {
        val cachetToken = cache[scope]?.get(onBehalfOf)

        return if (cachetToken == null) {
            null
        } else {
            val erFremdelesGyldigOmEttSekund = cachetToken.expires.isAfter(LocalDateTime.now().plusSeconds(1))
            if (erFremdelesGyldigOmEttSekund) cachetToken.token else null
        }
    }

    fun lagreOBOToken(scope: String, onBehalfOf: String, azureResponse: AzureResponse) {
        if (cache[scope] == null) {
            cache[scope] = HashMap()
        }

        val rettFørTokenetUtløper = LocalDateTime.now().plusSeconds(
            azureResponse.expires_in.toLong() - 10
        )

        cache[scope]!![onBehalfOf] = CachedToken(
            azureResponse.access_token,
            rettFørTokenetUtløper
        )
    }
}

data class CachedToken(
    val token: String,
    val expires: LocalDateTime,
)
