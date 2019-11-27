package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class StillingService(
        val restTemplate: RestTemplate,
        val externalConfiguration: ExternalConfiguration,
        val stillingsinfoService: StillingsinfoService,
        val tokenUtils: TokenUtils
) {

    fun hentStilling(uuid: String): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$uuid"
        LOG.debug("henter stilling fra url $url")
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headersUtenToken()),
                StillingMedStillingsinfo::class.java
        )
                .body
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stilling")

        return opprinneligStilling.medStillingsinfo(hentStillingsinfo(opprinneligStilling))
    }

    fun hentStillinger(url: String, queryString: String?): Page<StillingMedStillingsinfo> {
        val opprinneligeStillinger: Page<StillingMedStillingsinfo> = hent(url, queryString)
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stillinger")

        return opprinneligeStillinger.copy(content = opprinneligeStillinger.content
                .map { it.medStillingsinfo(hentStillingsinfo(it)) })
    }

    private fun hent(url: String, queryString: String?): Page<StillingMedStillingsinfo>? {
        val withQueryParams: String = UriComponentsBuilder.fromHttpUrl(url).query(queryString).build().toString()
        LOG.debug("henter stilling fra url $withQueryParams")
        return restTemplate.exchange(
                withQueryParams,
                HttpMethod.GET,
                HttpEntity(null, headers()),
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        ).body
    }

    private fun hentStillingsinfo(stillingMedStillingsinfo: StillingMedStillingsinfo): Option<Stillingsinfo> =
            stillingsinfoService.hentForStilling(Stillingsid(stillingMedStillingsinfo.uuid!!))

    private fun headers() =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
            ).toMultiValueMap()

    private fun headersUtenToken() =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString()
            ).toMultiValueMap()
}
