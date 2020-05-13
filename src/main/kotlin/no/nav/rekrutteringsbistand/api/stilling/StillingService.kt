package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import javax.servlet.http.HttpServletRequest

@Service
class StillingService(
        val restTemplate: RestTemplate,
        val externalConfiguration: ExternalConfiguration,
        val stillingsinfoService: StillingsinfoService,
        val tokenUtils: TokenUtils,
        val kandidatlisteKlient: KandidatlisteKlient,
        val restProxy: RestProxy
) {

    fun hentStilling(uuid: String): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$uuid"
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

    fun hentStillingMedStillingsnummer(stillingsnummer: String): StillingMedStillingsinfo {
        val stillingPage = hent("${externalConfiguration.stillingApi.url}/b2b/api/v1/ads", "id=${stillingsnummer}")

        if(stillingPage == null || stillingPage.content.isEmpty()) {
            throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stilling")
        } else {
            val stilling = stillingPage.content.first()
            val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(stilling)
            return stillingsinfo.map { stilling.copy(rekruttering = it.asDto()) }.getOrElse { stilling }
        }
    }

    fun opprettStilling(stilling: Stilling, queryString: String?): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads"
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
                url + if (queryString != null) "?$queryString" else "",
                HttpMethod.POST,
                HttpEntity(stilling, headers()),
                StillingMedStillingsinfo::class.java
        )
                .body
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra opprett stilling")

        val id = opprinneligStilling.uuid?.let { Stillingsid(it) }
                ?: throw IllegalArgumentException("Mangler stilling uuid")
        kandidatlisteKlient.oppdaterKandidatliste(id)
        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asDto()) }.getOrElse { opprinneligStilling }
    }

    fun oppdaterStilling(uuid: String, stilling: Stilling, queryString: String?): StillingMedStillingsinfo? {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/${uuid}"
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
                url + if (queryString != null) "?$queryString" else "",
                HttpMethod.PUT,
                HttpEntity(stilling, headers()),
                StillingMedStillingsinfo::class.java
        )
                .body
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra oppdater stilling")

        val id = opprinneligStilling.uuid?.let { Stillingsid(it) }
                ?: throw IllegalArgumentException("Mangler stilling uuid")
        kandidatlisteKlient.oppdaterKandidatliste(id)
        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asDto()) }.getOrElse { opprinneligStilling }
    }

    fun slettStilling(uuid: String, request: HttpServletRequest): ResponseEntity<String> {
        val respons = restProxy.proxyJsonRequest(HttpMethod.DELETE, request, "/rekrutteringsbistand-api/rekrutteringsbistand", null, externalConfiguration.stillingApi.url)
        kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(uuid))
        return respons
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
