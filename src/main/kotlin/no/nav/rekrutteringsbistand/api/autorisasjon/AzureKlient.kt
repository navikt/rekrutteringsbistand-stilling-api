package no.nav.rekrutteringsbistand.api.autorisasjon

import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.io.EOFException

const val systembrukerCacheKey = "system"

@Component
class AzureKlient(
    private val restTemplate: RestTemplate,
    private val azureCache: AzureCache,
    @Value("\${AZURE_OPENID_CONFIG_TOKEN_ENDPOINT}") private val tokenEndpoint: String,
    @Value("\${AZURE_APP_CLIENT_ID}") private val clientId: String,
    @Value("\${AZURE_APP_CLIENT_SECRET}") private val clientSecret: String
) {
    fun hentOBOToken(scope: String, onBehalfOf: String, assertionToken: String): String {
        val cachedToken = azureCache.hentOBOToken(scope, onBehalfOf)
        if (cachedToken != null) {
            return cachedToken
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val form = lagFormForOboRequest(scope, assertionToken)

        val response = try {
            restTemplate.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                HttpEntity(form, headers),
                AzureResponse::class.java
            )
        } catch (e: EOFException) {
            log.error("SSL-feil mot kall til arbeidsplassen", e)
            throw e
        }

        val responseBody = response.body

        if (responseBody != null) {
            azureCache.lagreOBOToken(scope, onBehalfOf, responseBody)
            return responseBody.access_token
        } else {
            throw Exception("Fikk ikke OBO-token fra azure")
        }
    }

    fun hentSystemToken(scope: String): String {
        val cachedToken = azureCache.hentOBOToken(scope, systembrukerCacheKey)
        if (cachedToken != null) {
            return cachedToken
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        val form = lagFormForSystemRequest(scope)
        try {
            val response = restTemplate.exchange(
                tokenEndpoint,
                HttpMethod.POST,
                HttpEntity(form, headers),
                AzureResponse::class.java
            )

            val responseBody = response.body!!
            azureCache.lagreOBOToken(scope, systembrukerCacheKey, responseBody)
            return responseBody.access_token
        } catch (exception: RestClientException) {
            throw Exception("Fikk ikke system-token fra azure:", exception)
        }
    }

    private fun lagFormForOboRequest(scope: String, assertionToken: String) = mapOf(
        "grant_type" to "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "scope" to scope,
        "client_id" to clientId,
        "client_secret" to clientSecret,
        "assertion" to assertionToken,
        "requested_token_use" to "on_behalf_of"
    ).toMultiValueMap()

    private fun lagFormForSystemRequest(scope: String) = mapOf(
        "grant_type" to "client_credentials",
        "scope" to scope,
        "client_id" to clientId,
        "client_secret" to clientSecret,
    ).toMultiValueMap()
}

data class AzureResponse(
    val access_token: String,
    val expires_in: Int,
)

data class AzureFailureResponse(
    val error: String,
    val error_description: Int,
)
