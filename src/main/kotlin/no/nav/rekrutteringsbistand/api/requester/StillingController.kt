package no.nav.rekrutteringsbistand.api.requester

import no.nav.rekrutteringsbistand.api.LOG
import no.nav.rekrutteringsbistand.api.konfigurasjon.Configuration
import no.nav.rekrutteringsbistand.api.konfigurasjon.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.requester.support.RestProxy
import no.nav.rekrutteringsbistand.api.requester.support.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.requester.support.TokenUtils
import no.nav.rekrutteringsbistand.api.requester.support.stillingDomene.Metadata
import no.nav.rekrutteringsbistand.api.requester.support.stillingDomene.Stilling
import no.nav.rekrutteringsbistand.api.toMultiValueMap
import no.nav.security.oidc.api.Protected
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@Protected
class StillingController(
        val restProxy: RestProxy,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val externalConfiguration: ExternalConfiguration,
        val stillingService: StillingService) {

    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    fun stilling(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> {
        return restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body
                ?: "", externalConfiguration.stillingApi.url)
    }

    @RequestMapping("/search-api/**")
    private fun sok(method: HttpMethod, request: HttpServletRequest, @RequestBody body: String = ""): ResponseEntity<String> =
            restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body, externalConfiguration.stillingApi.url)

    @GetMapping("/rekrutteringsbistand/api/v1/stilling/{uuid}")
    fun hentStilling(@PathVariable uuid: String, request: HttpServletRequest) : ResponseEntity<Stilling> {
        return ResponseEntity.ok().body(stillingService.hentStilling(uuid))
    }

}

@Service
class StillingService(
        restTemplateBuilder: RestTemplateBuilder,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val externalConfiguration: ExternalConfiguration,
        val rekrutteringsbistandService: RekrutteringsbistandService,
        val tokenUtils: TokenUtils) {

    var restTemplate = restTemplateBuilder.build()

    fun hentStilling(uuid: String) : Stilling {
        val url =  "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads/$uuid"
        LOG.debug("henter stilling fra url ${url}")
        val opprinneligStilling = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity(null,
                        mapOf(
                                HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                                HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString(),
                                HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
                        ).toMultiValueMap()),
                Stilling::class.java)
                .body

        val validertStilling = opprinneligStilling?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stilling")

        val rekruttering: Rekrutteringsbistand
        try {
            LOG.debug("henter rekrutteringsinformasjon for uuid ${uuid}")
            rekruttering = rekrutteringsbistandService.hentForStilling(uuid);
            LOG.debug("fant rekrutteringsinformasjon for uuid ${uuid}")
        } catch(er: EmptyResultDataAccessException) {
            return validertStilling;
        }


        val administrasjon = validertStilling.administration ?: return validertStilling

        return validertStilling.copy(
                administration = Metadata(status = administrasjon.status, comments = administrasjon.comments, reportee = rekruttering.eierNavn,
                        remarks = administrasjon.remarks, navIdent = rekruttering.eierIdent)
        )
    }
}



