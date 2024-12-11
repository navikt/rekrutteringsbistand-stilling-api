package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KandidatlisteKlient(
    private val restTemplate: RestTemplate,
    private val externalConfiguration: ExternalConfiguration,
    private val tokenUtils: TokenUtils,
    @Value("\${scope.kandidat-api}")
    private val scopeTilKandidatApi: String
) {

    fun sendStillingOppdatert(stilling: RekrutteringsbistandStilling): ResponseEntity<Void> {
        fun send(): ResponseEntity<Void> {
            val url = byggUrlTilPutEndepunkt()
            return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                HttpEntity(stilling, headers()),
                Void::class.java
            )
        }

        fun sendMedLogging(): ResponseEntity<Void> {
            try {
                val re = send()
                if (re.statusCode != HttpStatus.NO_CONTENT) {
                    log.warn(
                        "Uventet response fra kandidatliste-api for ad {}: {}",
                        stilling.stilling.uuid,
                        re.statusCode.value()
                    )
                }
                return re
            } catch (e: RestClientResponseException) {
                if (e.statusCode.isSameCodeAs(HttpStatus.FORBIDDEN)) {
                    val msg =
                        "Mangler tilgang til å oppdatere stilling. stilingsId=[${stilling.stilling.uuid}], innloggetVeileder=[${tokenUtils.hentInnloggetVeileder()}]"
                    log.warn(msg, e)
                }
                throw e
            }
        }

        return sendMedLogging()
    }

    fun varsleOmSlettetStilling(stillingsid: Stillingsid): ResponseEntity<Void> {
        val url: URI = byggUrlTilDeleteEndepunkt(stillingsid)
        val httpMethod = HttpMethod.DELETE
        log.info("Skal slette kandidatliste med stillingsid $stillingsid ved å sende en HTTP $httpMethod til URL $url")
        return try {
            restTemplate.exchange(
                url,
                httpMethod,
                HttpEntity(null, headers()),
                Void::class.java
            ).also {
                log.info("Varsle kandidatliste om sletting av stilling ${stillingsid.asString()} returnerte ${it.statusCode}")
            }
        } catch (e: HttpClientErrorException.NotFound) {
            log.info("$e", e)
            ResponseEntity.notFound().build()
        } catch (e: RestClientResponseException) {
            log.warn(
                "Uventet response fra kandidatliste-api for ad {}: {}",
                stillingsid.asString(),
                e.statusCode.value()
            )
            throw e
        }
    }

    private fun byggUrlTilDeleteEndepunkt(stillingsid: Stillingsid): URI {
        return UriComponentsBuilder.fromUriString(externalConfiguration.kandidatlisteApi.url)
            .pathSegment(stillingsid.asString())
            .pathSegment("kandidatliste")
            .build(true)
            .toUri()
    }

    private fun byggUrlTilPutEndepunkt(): URI {
        return UriComponentsBuilder.fromUriString(externalConfiguration.kandidatlisteApi.url)
            .pathSegment("kandidatliste")
            .build(true)
            .toUri()
    }

    fun headers() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOBOToken(scopeTilKandidatApi)}"
        ).toMultiValueMap()
}
