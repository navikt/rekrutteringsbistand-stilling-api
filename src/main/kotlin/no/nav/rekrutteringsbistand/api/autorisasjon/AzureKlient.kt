package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AzureKlient(
    private val restTemplate: RestTemplate,
    @Value("\${AZURE_OPENID_CONFIG_TOKEN_ENDPOINT}") private val tokenEndpoint: String,
    @Value("\${AZURE_APP_CLIENT_ID}") private val clientId: String,
    @Value("\${AZURE_APP_CLIENT_SECRET}") private val clientSecret: String
) {
    fun hentOBOToken(scope: String, assertionToken: String): String{
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val form = lagForm(scope, assertionToken)

        val loggbarForm = lagForm(scope, assertionToken.split(".")[1]).apply {
            this.set("client_secret", this["client_secret"]?.size.toString())
        }

        log.info("Kall til Azure sendes med form $loggbarForm")

        val response = restTemplate.exchange(
            tokenEndpoint,
            HttpMethod.POST,
            HttpEntity(form, headers),
            AzureResponse::class.java
        )

        log.info("Kall til Azure for OBO-token ga response ${response.statusCode}")
        return response.body?.access_token ?: throw Exception("Fikk ikke OBO-token fra azure")
    }

    private fun lagForm(scope: String, assertionToken: String) = mapOf(
        "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "scope" to scope,
        "client_id" to clientId,
        "client_secret" to clientSecret,
        "assertion" to assertionToken,
        "requested_token_use" to "on_behalf_of"
    ).toMultiValueMap()
}

private data class AzureResponse(
    val access_token: String
)