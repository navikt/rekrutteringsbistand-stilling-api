package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@Component
class ArbeidsplassenKlient(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val tokenUtils: TokenUtils,
) {
    fun hentStilling(stillingsId: String): Stilling {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$stillingsId"

        try {
            val respons = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, httpHeadersUtenToken()),
                Stilling::class.java
            )

            return respons.body ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke hente stilling fra arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke hente stillingen med stillingsId $stillingsId fra Arbeidsplassen"
            )
        }
    }

    fun hentStillingBasertPåAnnonsenr(annonsenr: String): Stilling {
        val url = UriComponentsBuilder
            .fromHttpUrl("${externalConfiguration.stillingApi.url}/b2b/api/v1/ads")
            .query("id=${annonsenr}")
            .build()
            .toString()

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, httpHeaders()),
                object : ParameterizedTypeReference<Page<Stilling>>() {}
            )

            return response.body?.content?.firstOrNull() ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke hente stilling fra arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke hente stillingen med annonsenr $annonsenr fra Arbeidsplassen"
            )
        }
    }

    fun hentMineStillinger(queryString: String?): Page<Stilling> {
        val url = UriComponentsBuilder
            .fromHttpUrl("${externalConfiguration.stillingApi.url}/api/v1/ads/rekrutteringsbistand/minestillinger")
            .query(queryString)
            .build()
            .toString()

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, httpHeaders()),
                object : ParameterizedTypeReference<Page<Stilling>>() {}
            )

            return response.body ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke hente mine stillinger fra arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke hente mine stillinger fra Arbeidsplassen"
            )
        }
    }

    fun opprettStilling(stilling: OpprettStillingDto): Stilling {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads?classify=true"

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(stilling, httpHeaders()),
                Stilling::class.java
            )

            return response.body ?: throw kunneIkkeTolkeBodyException()
        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke å opprette stilling hos arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke å opprette stilling Arbeidsplassen"
            )
        }
    }

    fun oppdaterStilling(stilling: Stilling, queryString: String?): Stilling {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/${stilling.uuid}"

        try {
            val response = restTemplate.exchange(
                url + if (queryString != null) "?$queryString" else "",
                HttpMethod.PUT,
                HttpEntity(stilling, httpHeaders()),
                Stilling::class.java
            )

            return response.body ?: throw kunneIkkeTolkeBodyException()
        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke å oppdatere stilling hos arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke å oppdatere stilling Arbeidsplassen"
            )
        }
    }

    fun slettStilling(stillingsId: String): Stilling {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/$stillingsId"

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                HttpEntity(null, httpHeaders()),
                Stilling::class.java
            )

            return response.body ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            LOG.error("Klarte ikke å slette stilling hos arbeidsplassen. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}")
            throw ResponseStatusException(
                HttpStatus.valueOf(exception.rawStatusCode),
                "Klarte ikke å slette stilling hos Arbeidsplassen, stillingsId $stillingsId"
            )
        }
    }

    private fun kunneIkkeTolkeBodyException(): ResponseStatusException {
        return ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Klarte ikke å tolke respons fra Arbeidsplassen")
    }

    private fun httpHeaders() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
        ).toMultiValueMap()

    private fun httpHeadersUtenToken() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE
        ).toMultiValueMap()
}
