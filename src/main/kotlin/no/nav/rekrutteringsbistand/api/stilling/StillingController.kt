package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.config.Configuration
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpMethod
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

    @PostMapping("/rekrutteringsbistand/api/v1/ads/")
    fun proxyPostTilStillingsApi(request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.opprettStilling(stilling, request.queryString))
    }

    @PutMapping("/rekrutteringsbistand/api/v1/ads/{uuid}")
    fun proxyPutTilStillingsApi(@PathVariable uuid: String, request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.oppdaterStilling(uuid, stilling, request.queryString))
    }

    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    fun proxyGetTilStillingsApi(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> {
        return restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body
                ?: "", externalConfiguration.stillingApi.url)
    }

    @RequestMapping("/search-api/**")
    private fun proxySokTilStillingsApi(method: HttpMethod, request: HttpServletRequest, @RequestBody requestBody: String?): ResponseEntity<String> {
        val respons = restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, requestBody
                ?: "", externalConfiguration.stillingApi.url) // TODO Are ""?
        val responsBody: String = respons.body ?: ""
        return ResponseEntity(responsBody, respons.statusCode)
    }

    @Unprotected // Fordi kandidatsøk har hentet stillinger uten token frem til nå.
    @GetMapping("/rekrutteringsbistand/api/v1/stilling/{uuid}")
    fun hentStilling(@PathVariable uuid: String, request: HttpServletRequest): ResponseEntity<StillingMedStillingsinfo> {
        return ResponseEntity.ok().body(stillingService.hentStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads")
    fun hentStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        return ResponseEntity.ok().body(stillingService.hentStillinger(
                    "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads",
                    if (request.queryString != null) URLDecoder.decode(request.queryString, StandardCharsets.UTF_8) else null

            ))
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger")
    fun hentMineStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        return ResponseEntity.ok().body(stillingService.hentStillinger(
                    "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                    if (request.queryString != null) URLDecoder.decode(request.queryString, StandardCharsets.UTF_8) else null
            ))
        }

}

