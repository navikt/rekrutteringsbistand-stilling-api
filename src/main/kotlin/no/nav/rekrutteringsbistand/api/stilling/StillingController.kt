package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest


@RestController
@Protected
class StillingController(
        val restProxy: RestProxy,
        val externalConfiguration: ExternalConfiguration,
        val stillingService: StillingService
) {
    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    @Deprecated("Skal erstattes av mer eksplisitte/spesifikke endepunktmetoder")
    fun proxyGetTilStillingsApi(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> {
        LOG.debug("Deprecated: Mottok $method til '/rekrutteringsbistand/api/v1/**' (${request.requestURI})")
        val respons = restProxy.proxyJsonRequest(method, request, replaceInUrl, body
                ?: "", externalConfiguration.stillingApi.url)
        val responsBody: String = respons.body ?: ""
        return ResponseEntity(responsBody, respons.statusCode)
    }

    @RequestMapping("/search-api/**")
    @Deprecated("Skal erstattes av mer eksplisitte/spesifikke endepunktmetoder")
    private fun proxySokTilStillingsApi(method: HttpMethod, request: HttpServletRequest, @RequestBody requestBody: String?): ResponseEntity<String> {
        LOG.debug("Deprecated: Mottok $method til '/search-api/**' (${request.requestURI})")
        val respons = restProxy.proxyJsonRequest(method, request, replaceInUrl, requestBody
                ?: "", externalConfiguration.stillingApi.url) // TODO Are ""?
        val responsBody: String = respons.body ?: ""
        return ResponseEntity(responsBody, respons.statusCode)
    }

    @PostMapping("/rekrutteringsbistand/api/v1/ads")
    fun proxyPostTilStillingsApi(request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.opprettStilling(stilling, request.queryString))
    }

    @PutMapping("/rekrutteringsbistand/api/v1/ads/{uuid}")
    fun proxyPutTilStillingsApi(@PathVariable uuid: String, request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.oppdaterStilling(uuid, stilling, request.queryString))
    }

    @DeleteMapping("/rekrutteringsbistand/api/v1/ads")
    fun proxyDeleteTilStillingsApi(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(DELETE, request, replaceInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand-api/rekrutteringsbistand/api/v1/geography/municipals")
    fun proxyGetMunicipals(method: HttpMethod, request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(method, request, replaceInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand-api/rekrutteringsbistand/api/v1/geography/counties")
    fun proxyGetCounties(method: HttpMethod, request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(method, request, replaceInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/search-api/underenhet/_search")
    private fun getSokTilPamAdApi(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @PostMapping("/search-api/underenhet/_search")
    private fun postSokTilPamAdApi(request: HttpServletRequest, @RequestBody requestBody: String): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(POST, request, replaceInUrl, requestBody, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @Unprotected // Fordi kandidatsøk har hentet stillinger uten token frem til nå.
    @GetMapping("/rekrutteringsbistand/api/v1/stilling/{uuid}")
    fun hentStilling(@PathVariable uuid: String, request: HttpServletRequest): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.hentStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads")
    fun hentStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        val url = "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads"
        val queryString: String? = request.queryString?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val page: Page<StillingMedStillingsinfo> = stillingService.hentStillinger(url, queryString)
        return ResponseEntity.ok(page)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger")
    fun hentMineStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        return ResponseEntity.ok().body(stillingService.hentStillinger(
                "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                if (request.queryString != null) URLDecoder.decode(request.queryString, StandardCharsets.UTF_8) else null
        ))
    }

}

private const val replaceInUrl = "/rekrutteringsbistand-api"
