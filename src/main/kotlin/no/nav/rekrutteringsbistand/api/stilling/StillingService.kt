package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
import arrow.core.getOrElse
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

        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asDto()) }.getOrElse { opprinneligStilling }
    }

    fun opprettStilling(stilling: Stilling, queryString: String?): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads"
        LOG.debug("Oppdaterer stilling med url $url/$queryString")
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
                url + queryString,
                HttpMethod.POST,
                HttpEntity(stilling, headers()),
                StillingMedStillingsinfo::class.java
        )
                .body
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra opprett stilling")

        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asDto()) }.getOrElse { opprinneligStilling }
    }

    fun oppdaterStilling(uuid: String, stilling: Stilling, queryString: String?): StillingMedStillingsinfo? {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/${uuid}"
        LOG.debug("oppretter stilling med url $url/$queryString")
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
                url + queryString,
                HttpMethod.PUT,
                HttpEntity(stilling, headers()),
                StillingMedStillingsinfo::class.java
        )
                .body
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra oppdater stilling")

        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asDto()) }.getOrElse { opprinneligStilling }
    }

    fun hentStillinger(url: String, queryString: String?): Page<StillingMedStillingsinfo> {
        val opprinneligeStillingerPage: Page<StillingMedStillingsinfo> = hent(url, queryString)
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stillinger")
        val opprinneligeStillinger = opprinneligeStillingerPage.content
        val stillingsinfoer = opprinneligeStillinger.map(::hentStillingsinfo)
        val newContent = stillingsinfoer.zip(opprinneligeStillinger, ::leggPåStillingsinfo)
        return opprinneligeStillingerPage.copy(content = newContent)
    }

    private fun leggPåStillingsinfo(info: Option<Stillingsinfo>, opprinnelig: StillingMedStillingsinfo): StillingMedStillingsinfo {
        return info.map { opprinnelig.copy(rekruttering = it.asDto()) }.getOrElse { opprinnelig }
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
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
            ).toMultiValueMap()

    private fun headersUtenToken() =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE
            ).toMultiValueMap()

}
