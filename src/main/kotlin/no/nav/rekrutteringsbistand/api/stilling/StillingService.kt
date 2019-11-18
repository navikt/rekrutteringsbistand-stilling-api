package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.rekrutteringsbistand.RekrutteringsbistandService
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.dao.EmptyResultDataAccessException
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
        val rekrutteringsbistandService: RekrutteringsbistandService,
        val tokenUtils: TokenUtils
) {

    fun hentStilling(uuid: String): Stilling {
        val url = "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads/$uuid"
        LOG.debug("henter stilling fra url $url")
        val opprinneligStilling = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null, headers()),
                Stilling::class.java)
                .body

        return berikMedRekruttering(
                opprinneligStilling ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stilling")
        )
    }

    fun hentStillinger(url: String, queryString: String?): Page<Stilling> {

        val withQueryParams: String = UriComponentsBuilder.fromHttpUrl(url).query(queryString).build().toString()

        LOG.debug("henter stilling fra url $withQueryParams")
        val opprinneligeStillinger = restTemplate.exchange(
                withQueryParams,
                HttpMethod.GET,
                HttpEntity(null, headers()),
                object : ParameterizedTypeReference<Page<Stilling>>() {})
                .body

        val validertContent = (opprinneligeStillinger
                ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stillinger")).content

        return opprinneligeStillinger.copy(
                content = validertContent
                        .map {
                            berikMedRekruttering(it)
                        })
    }

    fun berikMedRekruttering(stilling: Stilling): Stilling =
         try {
            LOG.debug("henter rekrutteringsinformasjon for uuid ${stilling.uuid}")
            val rekrutteringsbidstand = rekrutteringsbistandService.hentForStilling(stilling.uuid!!)
            LOG.debug("fant rekrutteringsinformasjon for uuid ${stilling.uuid}")
            stilling.copy(
                    rekruttering = rekrutteringsbidstand.asDto()
            )
        } catch (er: EmptyResultDataAccessException) {
            stilling
        }

    fun headers() =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
            ).toMultiValueMap()
}
