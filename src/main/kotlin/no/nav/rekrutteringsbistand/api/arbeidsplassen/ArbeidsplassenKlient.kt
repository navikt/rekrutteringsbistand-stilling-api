package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.UnknownContentTypeException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder

@Component
class ArbeidsplassenKlient(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val tokenUtils: TokenUtils,
    @Value("\${scope.forarbeidsplassen}") private val scopeMotArbeidsplassen: String
) {
    fun hentStilling(stillingsId: String, somSystembruker: Boolean = false): Stilling {
        val url = "${hentBaseUrl()}/b2b/api/v1/ads/$stillingsId"

        try {
            val respons = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(
                    null,
                    if (somSystembruker) httpHeadersSomSystembruker() else httpHeaders()
                ),
                Stilling::class.java
            )
            return respons.body ?: throw kunneIkkeTolkeBodyException()
        } catch (e: UnknownContentTypeException) {
            throw kunneIkkeTolkeBodyException(e)
        } catch (exception: RestClientResponseException) {
            throw svarMedFeilmelding(
                "Klarte ikke hente stillingen med stillingsId $stillingsId fra Arbeidsplassen",
                url,
                exception
            )
        }
    }

    /**
     * Dette er en "hack" for å sørge for at stillingssøket vårt alltid er oppdatert
     * med riktig stillingsinfo.
     *
     * Vi henter vi en stilling fra Arbeidsplassen og sender den tilbake til dem.
     * Dette gjør at de sender en ny Kafka-melding med stillingen. Vi lytter på denne
     * meldingen i stilling-indekseren vår, som vil hente oppdatert stillingsinfo og
     * oppdaterer indeksen.
     */
    fun triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsid: String) {
        val stilling = hentStilling(stillingsid)
        oppdaterStilling(stilling, null)

        log.info("Trigget resending av stillingsmelding fra Arbeidsplassen for stilling $stillingsid")
    }

    fun hentStillingBasertPåAnnonsenr(annonsenr: String): Stilling {
        val url = UriComponentsBuilder
            .fromHttpUrl("${hentBaseUrl()}/b2b/api/v1/ads")
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
            throw svarMedFeilmelding(
                "Klarte ikke hente stillingen med annonsenr $annonsenr fra Arbeidsplassen",
                url,
                exception
            )
        }
    }

    fun hentMineStillinger(queryString: String?): Page<Stilling> {
        val url = UriComponentsBuilder
            .fromHttpUrl("${hentBaseUrl()}/api/v1/ads/rekrutteringsbistand/minestillinger")
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
            throw svarMedFeilmelding("Klarte ikke hente mine stillinger fra Arbeidsplassen", url, exception)
        }
    }

    fun opprettStilling(stilling: OpprettStillingDto): Stilling {
        val url = "${hentBaseUrl()}/api/v1/ads?classify=true"

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(stilling, httpHeaders()),
                Stilling::class.java
            )
            return response.body ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            throw svarMedFeilmelding("Klarte ikke å opprette stilling hos Arbeidsplassen", url, exception)
        }
    }

    fun oppdaterStilling(stilling: Stilling, queryString: String?): Stilling {
        val url = "${hentBaseUrl()}/api/v1/ads/${stilling.uuid}"

        try {
            val response = restTemplate.exchange(
                url + if (queryString != null) "?$queryString" else "",
                HttpMethod.PUT,
                HttpEntity(stilling, httpHeaders()),
                Stilling::class.java
            )
            return response.body ?: throw kunneIkkeTolkeBodyException()

        } catch (exception: RestClientResponseException) {
            throw svarMedFeilmelding("Klarte ikke å oppdatere stilling hos Arbeidsplassen", url, exception)
        }
    }

    fun slettStilling(stillingsId: String): Stilling {
        val url = "${hentBaseUrl()}/api/v1/ads/$stillingsId"

        try {
            val response = restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                HttpEntity(null, httpHeaders()),
                Stilling::class.java
            )
            return response.body ?: throw kunneIkkeTolkeBodyException()
        } catch (e: UnknownContentTypeException) {
            throw kunneIkkeTolkeBodyException(e)
        } catch (exception: RestClientResponseException) {
            throw svarMedFeilmelding("Klarte ikke å slette stilling hos arbeidsplassen", url, exception)
        }
    }

    private fun svarMedFeilmelding(
        melding: String,
        url: String,
        exception: RestClientResponseException
    ): ResponseStatusException {
        val logMsg = "$melding. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}"
        when (exception.rawStatusCode) {
            NOT_FOUND.value() -> log.warn(logMsg, exception)
            PRECONDITION_FAILED.value() -> log.info(logMsg, exception)
            else -> log.error(logMsg, exception)
            // 412 får vi når noen prøver å endre en stilling, men en annen har endret stillingen samtidig. pam-ad sjekker “updated”-timestampen i databasen og sørger for at “updated”-timestampen som kommer inn ikke er eldre enn den som er lagret
        }
        return ResponseStatusException(valueOf(exception.rawStatusCode), melding)
    }

    private fun kunneIkkeTolkeBodyException(cause: Throwable? = null): ResponseStatusException {
        return ResponseStatusException(
            INTERNAL_SERVER_ERROR,
            "Klarte ikke å tolke respons fra Arbeidsplassen",
            cause
        )
    }

    private fun hentBaseUrl() = externalConfiguration.pamAdApiGcp.url

    private fun httpHeaders() =
        mapOf(
            CONTENT_TYPE to APPLICATION_JSON_VALUE,
            ACCEPT to APPLICATION_JSON_VALUE,
            AUTHORIZATION to "Bearer ${tokenUtils.hentOBOToken(scopeMotArbeidsplassen)}"
        ).toMultiValueMap()

    private fun httpHeadersSomSystembruker() =
        mapOf(
            CONTENT_TYPE to APPLICATION_JSON_VALUE,
            ACCEPT to APPLICATION_JSON_VALUE,
            AUTHORIZATION to "Bearer ${tokenUtils.hentSystemToken(scopeMotArbeidsplassen)}"
        ).toMultiValueMap()
}
