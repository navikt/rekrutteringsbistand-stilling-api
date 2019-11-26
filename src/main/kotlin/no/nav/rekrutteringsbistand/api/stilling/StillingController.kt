package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.config.Configuration
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.security.oidc.api.Protected
import no.nav.security.oidc.api.Unprotected
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
@Protected
class StillingController(
        val restProxy: RestProxy,
        val externalConfiguration: ExternalConfiguration,
        val stillingService: StillingService
) {

    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    fun stilling(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> {
        return restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body
                ?: "", externalConfiguration.stillingApi.url)
    }

    @RequestMapping("/search-api/**")
    private fun proxySokTilStillingsApi(method: HttpMethod, request: HttpServletRequest, @RequestBody body: String?): ResponseEntity<String> =
            restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body ?: "", externalConfiguration.stillingApi.url)

    @Unprotected // Fordi kandidatsøk har hentet stillinger uten token frem til nå.
    @GetMapping("/rekrutteringsbistand/api/v1/stilling/{uuid}")
    fun hentStilling(@PathVariable uuid: String, request: HttpServletRequest): ResponseEntity<Stilling> {
        return ResponseEntity.ok().body(stillingService.hentStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads")
    fun hentStillinger(request: HttpServletRequest): ResponseEntity<Page<Stilling>> {
        return ResponseEntity.ok().body(stillingService.hentStillinger(
                "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads",
                request.queryString
        ))
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger")
    fun hentMineStillinger(request: HttpServletRequest): ResponseEntity<Page<Stilling>> {
        return ResponseEntity.ok().body(stillingService.hentStillinger(
                "${externalConfiguration.stillingApi.url}/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                request.queryString
        ))
    }

}

