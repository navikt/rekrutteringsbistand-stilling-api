package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Component
class KandidatlisteKlient(
        val restTemplate: RestTemplate,
        val externalConfiguration: ExternalConfiguration
) {

    fun oppdaterKandidatliste(stillingsid: Stillingsid): ResponseEntity<Void> {
        val url = buildUpdateNotificationUrl(stillingsid)
        LOG.info("Oppdaterer kandidatliste, stillingsid: $stillingsid")
        return try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    HttpEntity(null, headers()),
                    Void::class.java
            )
                    .also {
                        if (it.statusCode != HttpStatus.NO_CONTENT) {
                            LOG.warn("Uventet response fra kandidatliste-api for ad {}: {}", stillingsid.asString(), it.statusCodeValue)
                        }
                    }
        } catch (t: Throwable) {
            LOG.error("oppdatering av kandidatliste med stillingsid $stillingsid feilet")
            throw t
        }
    }

    private fun buildUpdateNotificationUrl(stillingsid: Stillingsid): URI {
        return UriComponentsBuilder.fromUriString(externalConfiguration.kandidatlisteApi.url)
                .pathSegment(stillingsid.asString())
                .pathSegment("kandidatliste")
                .build(true)
                .toUri()
    }

    fun headers() =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString()
            ).toMultiValueMap()
}
