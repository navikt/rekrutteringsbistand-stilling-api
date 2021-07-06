package no.nav.rekrutteringsbistand.api.stilling.ekstern

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ArbeidsplassenKlient(
    private val restTemplate: RestTemplate,
    private val tokenUtils: TokenUtils,
    externalConfiguration: ExternalConfiguration
) {
    private val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/"

    fun triggeNyStillingsMeldingFraArbeidsplassen(
        stilling: Stilling,
        queryString: String?
    ): ResponseEntity<Stilling> =
        lagre(stilling, queryString)

    private fun lagre(
        stilling: Stilling,
        queryString: String?
    ) =
        restTemplate.exchange(
            byggUrl(url, stilling.uuid!!, queryString),
            HttpMethod.PUT,
            HttpEntity(stilling, headers()),
            Stilling::class.java
        )

    private fun byggUrl(
        url: String,
        stillingsId: String,
        queryString: String?
    ) =
        "$url${stillingsId}${if (queryString != null) "?$queryString" else ""}"

    fun hentStilling(
        stillingsId: String,
        queryString: String?
    ): ResponseEntity<Stilling> =
        restTemplate.exchange(
            byggUrl(url, stillingsId, queryString),
            HttpMethod.GET,
            HttpEntity<Any>(headers()),
            Stilling::class.java
        )

    private fun headers() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
        ).toMultiValueMap()
}
