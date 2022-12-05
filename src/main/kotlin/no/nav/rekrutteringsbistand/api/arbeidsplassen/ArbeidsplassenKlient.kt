package no.nav.rekrutteringsbistand.api.arbeidsplassen

import arrow.core.Option
import arrow.core.firstOrNone
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.executeFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
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
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.*
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.UriComponentsBuilder
import java.io.EOFException
import java.io.IOException
import java.time.Duration
import java.util.*


@Component
class ArbeidsplassenKlient(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val tokenUtils: TokenUtils,
    @Value("\${scope.forarbeidsplassen}") private val scopeMotArbeidsplassen: String
) {

    fun hentStilling(stillingsId: String, somSystembruker: Boolean = false): Stilling {
        val url = "${hentBaseUrl()}/b2b/api/v1/ads/$stillingsId"

        val hent: () -> ResponseEntity<Stilling> = {
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, if (somSystembruker) httpHeadersSomSystembruker() else httpHeaders()),
                Stilling::class.java
            )
        }

        val hentMedFeilhåndtering: () -> Stilling = {
            try {
                val respons = retry.executeFunction(hent)
                respons.body ?: throw kunneIkkeTolkeBodyException()
            } catch (e: UnknownContentTypeException) {
                throw svarMedFeilmelding(
                    "Klarte ikke hente stillingen med stillingsId $stillingsId fra Arbeidsplassen",
                    url,
                    e
                )
            } catch (e: RestClientResponseException) {
                throw svarMedFeilmelding(
                    "Klarte ikke hente stillingen med stillingsId $stillingsId fra Arbeidsplassen",
                    url,
                    e
                )
            } catch (e: Exception) {
                fun logErrorIfEofexception(t: Throwable?) {
                    when (t) {
                        null -> return
                        is EOFException -> log.error("SSL-feil mot kall til arbeidsplassen", e)
                        else -> logErrorIfEofexception(t.cause)
                    }
                }
                logErrorIfEofexception(e) // Lagt til for å lett kunne se i apploggen hvor ofte vi får denne feilen på dette endepunktet. November 2022.
                throw e
            }
        }

        return timer(
            "rekrutteringsbistand.stilling.arbeidsplassen.hentStilling.kall.tid",
            hentMedFeilhåndtering
        )
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
    fun triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsid: String) =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.triggResendingAvStillingsmeldingFraArbeidsplassen.kall.tid") {
            val stilling = hentStilling(stillingsid)
            oppdaterStilling(stilling, null)

            log.info("Trigget resending av stillingsmelding fra Arbeidsplassen for stilling $stillingsid")
        }

    fun hentStillingBasertPåAnnonsenr(annonsenr: String): Option<Stilling> =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.hentStillingBasertPåAnnonsenr.kall.tid") {
            val url =
                UriComponentsBuilder.fromHttpUrl("${hentBaseUrl()}/b2b/api/v1/ads").query("id=${annonsenr}")
                    .build()
                    .toString()

            try {
                val response = restTemplate.exchange(url,
                    HttpMethod.GET,
                    HttpEntity(null, httpHeaders()),
                    object : ParameterizedTypeReference<Page<Stilling>>() {})
                return@timer response.body?.content?.firstOrNone() ?: throw kunneIkkeTolkeBodyException()

            } catch (exception: RestClientResponseException) {
                throw svarMedFeilmelding(
                    "Klarte ikke hente stillingen med annonsenr $annonsenr fra Arbeidsplassen", url, exception
                )
            }
        }

    fun hentStillingBasertPåUUID(uuid: String): Option<Stilling> =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.hentStillingBasertPåUUID.kall.tid") {
            val url = UriComponentsBuilder.fromHttpUrl("${hentBaseUrl()}/b2b/api/v1/ads").query("uuid=${uuid}")
                .build()
                .toString()
            try {
                val response: ResponseEntity<Page<Stilling>> = restTemplate.exchange(url,
                    HttpMethod.GET,
                    HttpEntity(null, httpHeadersSomSystembruker()),
                    object : ParameterizedTypeReference<Page<Stilling>>() {})
                return@timer response.body?.content?.firstOrNone() ?: throw kunneIkkeTolkeBodyException()

            } catch (e: RestClientResponseException) {
                throw svarMedFeilmelding(
                    "Klarte ikke hente stillingen med uuid $uuid fra Arbeidsplassen",
                    url,
                    e
                )
            } catch (e: UnknownContentTypeException) {
                throw svarMedFeilmelding(
                    "Klarte ikke hente stillingen med uuid $uuid fra Arbeidsplassen",
                    url,
                    e
                )
            }
        }

    fun hentMineStillinger(queryString: String?): Page<Stilling> =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.hentMineStillinger.kall.tid") {
            val url =
                UriComponentsBuilder.fromHttpUrl("${hentBaseUrl()}/api/v1/ads/rekrutteringsbistand/minestillinger")
                    .query(queryString).build().toString()

            try {
                val response = restTemplate.exchange(url,
                    HttpMethod.GET,
                    HttpEntity(null, httpHeaders()),
                    object : ParameterizedTypeReference<Page<Stilling>>() {})
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()

            } catch (e: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke hente mine stillinger fra Arbeidsplassen", url, e)
            } catch (e: RestClientException) {
                throw ResponseStatusException(
                    INTERNAL_SERVER_ERROR, "Klarte ikke hente mine stillinger fra Arbeidsplassen", e
                )
            }
        }

    fun opprettStilling(stilling: OpprettStillingDto): Stilling =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.opprettStilling.kall.tid") {
            val url = "${hentBaseUrl()}/api/v1/ads?classify=true"

            try {
                val response = restTemplate.exchange(
                    url, HttpMethod.POST, HttpEntity(stilling, httpHeaders()), Stilling::class.java
                )
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()

            } catch (exception: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke å opprette stilling hos Arbeidsplassen", url, exception)
            }
        }

    fun oppdaterStilling(stilling: Stilling, queryString: String?): Stilling =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.oppdaterStilling.kall.tid") {
            val url = "${hentBaseUrl()}/api/v1/ads/${stilling.uuid}"

            try {
                val response = restTemplate.exchange(
                    url + if (queryString != null) "?$queryString" else "",
                    HttpMethod.PUT,
                    HttpEntity(stilling, httpHeaders()),
                    Stilling::class.java
                )
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()

            } catch (exception: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke å oppdatere stilling hos Arbeidsplassen", url, exception)
            }
        }

    fun slettStilling(stillingsId: String): Stilling =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.slettStilling.kall.tid") {
            val url = "${hentBaseUrl()}/api/v1/ads/$stillingsId"

            try {
                val response = restTemplate.exchange(
                    url, HttpMethod.DELETE, HttpEntity(null, httpHeaders()), Stilling::class.java
                )
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()
            } catch (e: UnknownContentTypeException) {
                throw svarMedFeilmelding("Klarte ikke å slette stilling hos arbeidsplassen", url, e)
            } catch (e: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke å slette stilling hos arbeidsplassen", url, e)
            }
        }

    private fun svarMedFeilmelding(
        melding: String, url: String, exception: RestClientResponseException
    ): ResponseStatusException {
        val logMsg =
            "$melding. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}"
        when (exception.rawStatusCode) {
            NOT_FOUND.value() -> log.warn(logMsg, exception)
            PRECONDITION_FAILED.value() -> log.info(logMsg, exception)
            else -> log.error(logMsg, exception)
            // 412 får vi når noen prøver å endre en stilling, men en annen har endret stillingen samtidig. pam-ad sjekker “updated”-timestampen i databasen og sørger for at “updated”-timestampen som kommer inn ikke er eldre enn den som er lagret
        }
        return ResponseStatusException(valueOf(exception.rawStatusCode), melding)
    }

    /**
     * Det er mulig å tenke seg at HTTP respons statsukode fra Arbeidsplassen er 200 ok samtidig som responsens content type er uventet.
     * Så her logger vi statuskoden mottatt fra Arbeidsplassen men returnerer _alltid_ statuskode 500 server error.
     */
    private fun svarMedFeilmelding(
        melding: String, url: String, exception: UnknownContentTypeException
    ): ResponseStatusException {
        val logMsg =
            "$melding. URL: $url, Status: ${exception.rawStatusCode}, Body: ${exception.responseBodyAsString}"
        log.error(logMsg, exception)
        return ResponseStatusException(INTERNAL_SERVER_ERROR, melding)
    }

    // Jeg mistenker at denne aldri blir kalt ved kjøretid. Are 2022-11-12
    private fun kunneIkkeTolkeBodyException(): ResponseStatusException =
        ResponseStatusException(INTERNAL_SERVER_ERROR, "Klarte ikke å tolke respons fra Arbeidsplassen")

    private fun hentBaseUrl() = externalConfiguration.pamAdApi.url

    private fun httpHeaders() = mapOf(
        CONTENT_TYPE to APPLICATION_JSON_VALUE,
        ACCEPT to APPLICATION_JSON_VALUE,
        AUTHORIZATION to "Bearer ${tokenUtils.hentOBOToken(scopeMotArbeidsplassen)}"
    ).toMultiValueMap()

    private fun httpHeadersSomSystembruker() = mapOf(
        CONTENT_TYPE to APPLICATION_JSON_VALUE,
        ACCEPT to APPLICATION_JSON_VALUE,
        AUTHORIZATION to "Bearer ${tokenUtils.hentSystemToken(scopeMotArbeidsplassen)}"
    ).toMultiValueMap()


    companion object {
        private fun <T> timer(timerName: String, toBeTimed: () -> T): T =
            Timer.builder(timerName).publishPercentiles(0.5, 0.75, 0.9, 0.99).publishPercentileHistogram()
                .minimumExpectedValue(Duration.ofMillis(1)).maximumExpectedValue(Duration.ofSeconds(61))
                .register(Metrics.globalRegistry).record(toBeTimed)!!

        private fun isIOException(t: Throwable?): Boolean =
            when (t) {
                null -> false
                is IOException -> true
                else -> isIOException(t.cause)
            }

        private val retry: Retry by lazy {
            val exponentialBackoff = IntervalFunction.ofExponentialBackoff(Duration.ofMillis(500), 3.0)
            val retryConfig = RetryConfig.custom<ResponseEntity<*>>()
                .maxAttempts(3)
                .intervalFunction(exponentialBackoff)
                .retryOnResult { it.statusCode.is5xxServerError }
                .retryOnException(this::isIOException)
                .retryExceptions(HttpServerErrorException::class.java, ResourceAccessException::class.java)
                .build()
            Retry.of("ArbeidsplassenKlient", retryConfig)
        }
    }

}
