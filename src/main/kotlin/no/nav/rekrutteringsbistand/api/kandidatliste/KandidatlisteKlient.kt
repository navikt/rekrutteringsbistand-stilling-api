package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
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

    fun varsleOmOppdatertStilling(stillingsid: Stillingsid): ResponseEntity<Void> {
        val url = buildNotificationUrl(stillingsid)
        log.info("Oppdaterer kandidatliste, stillingsid: $stillingsid")
        return restTemplate.exchange(
            url,
            HttpMethod.PUT,
            HttpEntity(null, headers()),
            Void::class.java
        )
            .also {
                if (it.statusCode != HttpStatus.NO_CONTENT) {
                    log.warn("Uventet response fra kandidatliste-api for ad {}: {}", stillingsid.asString(), it.statusCodeValue)
                }
            }
    }

    fun varsleOmSlettetStilling(stillingsid: Stillingsid): ResponseEntity<Void> {
        val url = buildNotificationUrl(stillingsid)
        log.info("Slett kandidatliste, stillingsid: $stillingsid")
        return restTemplate.exchange(
            url,
            HttpMethod.DELETE,
            HttpEntity(null, headers()),
            Void::class.java
        )
            .also {
                if (it.statusCode != HttpStatus.NO_CONTENT) {
                    log.warn("Uventet response fra kandidatliste-api for ad {}: {}", stillingsid.asString(), it.statusCodeValue)
                }
            }
    }

    private fun buildNotificationUrl(stillingsid: Stillingsid): URI {
        return UriComponentsBuilder.fromUriString(externalConfiguration.kandidatlisteApi.url)
            .pathSegment(stillingsid.asString())
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
