package no.nav.rekrutteringsbistand.api.arbeidsplassen

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.FrontendStilling
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.rest.RetrySpringRestTemplate.retry
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
import java.time.Duration
import java.util.UUID

@Component
class ArbeidsplassenKlient(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val tokenUtils: TokenUtils,
    @Value("\${scope.forarbeidsplassen}") private val scopeMotArbeidsplassen: String
) {

    fun hentStilling(stillingsId: String, somSystembruker: Boolean = false): ArbeidsplassenStillingDto {
        val url = "${hentBaseUrl()}/b2b/api/v1/ads/$stillingsId"

        val hent: () -> ResponseEntity<ArbeidsplassenStillingDto> = {
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, if (somSystembruker) httpHeadersSomSystembruker() else httpHeaders()),
                ArbeidsplassenStillingDto::class.java
            )
        }

        val hentMedFeilhåndtering: () -> ArbeidsplassenStillingDto = {
            try {
                val respons = retry(hent)
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
            log.info("Stillings som sendes til pam-ad ved opprettelse av kandidatliste: $stilling")

            oppdaterStilling(stilling, null)

            log.info("Trigget resending av stillingsmelding fra Arbeidsplassen for stilling $stillingsid")
        }

    fun hentStillingBasertPåUUID(uuid: String): FrontendStilling? =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.hentStillingBasertPåUUID.kall.tid") {
            val url = UriComponentsBuilder.fromHttpUrl("${hentBaseUrl()}/b2b/api/v1/ads").query("uuid=${uuid}")
                .build()
                .toString()
            try {
                val response: ResponseEntity<Page<FrontendStilling>> = restTemplate.exchange(url,
                    HttpMethod.GET,
                    HttpEntity(null, httpHeadersSomSystembruker()),
                    object : ParameterizedTypeReference<Page<FrontendStilling>>() {})
                return@timer response.body?.content?.firstOrNull() ?: throw kunneIkkeTolkeBodyException()

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

    fun opprettStilling(stilling: OpprettStillingDto): FrontendStilling =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.opprettStilling.kall.tid") {
            val url = "${hentBaseUrl()}/api/v1/ads?classify=true"

            try {
                val response = restTemplate.exchange(
                    url, HttpMethod.POST, HttpEntity(stilling, httpHeaders()), FrontendStilling::class.java
                )
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()

            } catch (exception: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke å opprette stilling hos Arbeidsplassen", url, exception)
            }
        }

    fun oppdaterStilling(stilling: ArbeidsplassenStillingDto, queryString: String?): ArbeidsplassenStillingDto =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.oppdaterStilling.kall.tid") {
            val url = "${hentBaseUrl()}/api/v1/ads/${stilling.uuid}"

            try {
                val response = restTemplate.exchange(
                    url + if (queryString != null) "?$queryString" else "",
                    HttpMethod.PUT,
                    HttpEntity(stilling, httpHeaders()),
                    ArbeidsplassenStillingDto::class.java
                )
                return@timer response.body ?: throw kunneIkkeTolkeBodyException()

            } catch (exception: RestClientResponseException) {
                throw svarMedFeilmelding("Klarte ikke å oppdatere stilling hos Arbeidsplassen", url, exception)
            }
        }

    fun slettStilling(stillingsId: String): FrontendStilling =
        timer("rekrutteringsbistand.stilling.arbeidsplassen.slettStilling.kall.tid") {
            val uuid = try {
                UUID.fromString(stillingsId)
            } catch (_: IllegalArgumentException) {
                throw ResponseStatusException(
                    BAD_REQUEST, "Ugyldig stillingsId. Må være en gyldig UUID."
                )
            }
            val url = "${hentBaseUrl()}/api/v1/ads/$uuid"

            try {
                val response = restTemplate.exchange(
                    url, HttpMethod.DELETE, HttpEntity(null, httpHeaders()), FrontendStilling::class.java
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
        when (exception.statusCode.value()) {
            NOT_FOUND.value() -> log.warn(logMsg, exception)
            PRECONDITION_FAILED.value() -> log.info(logMsg, exception)
            else -> log.error(logMsg, exception)
            // 412 får vi når noen prøver å endre en stilling, men en annen har endret stillingen samtidig. pam-ad sjekker “updated”-timestampen i databasen og sørger for at “updated”-timestampen som kommer inn ikke er eldre enn den som er lagret
        }
        return ResponseStatusException(valueOf(exception.statusCode.value()), melding)
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
    }

}
