package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.HentRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
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

    @PostMapping("/rekrutteringsbistand/api/v1/ads")
    fun proxyPostTilStillingsApi(request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        val opprettetStilling = stillingService.opprettStilling(stilling, request.queryString)
        return ok().body(opprettetStilling)
    }

    @PutMapping("/rekrutteringsbistand/api/v1/ads/{uuid}")
    @Deprecated("Bruk putRekrutteringsbistandStilling")
    fun proxyPutTilStillingsApi(@PathVariable uuid: String, request: HttpServletRequest, @RequestBody stilling: Stilling): ResponseEntity<StillingMedStillingsinfo> {
        val oppdatertStilling = stillingService.oppdaterStilling(uuid, stilling, request.queryString)
        return ok().body(oppdatertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun putRekrutteringsbistandStilling(request: HttpServletRequest, @RequestBody rekrutteringsbistandStillingDto: OppdaterRekrutteringsbistandStillingDto): ResponseEntity<OppdaterRekrutteringsbistandStillingDto> {
        val oppdatertStilling = stillingService.oppdaterRekrutteringsbistandStilling(rekrutteringsbistandStillingDto, request.queryString)
        return ok().body(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistand/api/v1/ads/{uuid}")
    fun proxyDeleteTilStillingsApi(request: HttpServletRequest, @PathVariable(value = "uuid") uuid: String): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons: ResponseEntity<String> = stillingService.slettStilling(uuid, request)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/geography/municipals")
    fun proxyGetMunicipals(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/geography/counties")
    fun proxyGetCounties(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/geography/countries")
    fun proxyGetCountries(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/categories-with-altnames")
    fun proxyGetCategoriesWithAltnames(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/postdata")
    fun proxyGetPostdata(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/search-api/underenhet/_search")
    private fun getSokTilPamAdApi(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(GET, request, "", null, externalConfiguration.sokApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @PostMapping("/search-api/underenhet/_search")
    private fun postSokTilPamAdApi(request: HttpServletRequest, @RequestBody requestBody: String): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = restProxy.proxyJsonRequest(POST, request, "", requestBody, externalConfiguration.sokApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/stilling/{uuid}")
    @Deprecated("Bruk hentRekrutteringsbistandStilling")
    fun hentStilling(@PathVariable uuid: String): ResponseEntity<StillingMedStillingsinfo> =
            ok(stillingService.hentStilling(uuid))

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: String): ResponseEntity<HentRekrutteringsbistandStillingDto> =
            ok(stillingService.hentRekrutteringsbistandStilling(uuid))

    @GetMapping("/rekrutteringsbistand/api/v1/stilling/stillingsnummer/{stillingsnummer}")
    fun hentStillingAnnonsenummer(@PathVariable stillingsnummer: String): ResponseEntity<StillingMedStillingsinfo> =
            ok(stillingService.hentStillingMedStillingsnummer(stillingsnummer))

    @GetMapping("/rekrutteringsbistand/api/v1/ads")
    fun hentStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads"
        val queryString: String? = request.queryString?.let { URLDecoder.decode(it, StandardCharsets.UTF_8) }
        val page: Page<StillingMedStillingsinfo> = stillingService.hentStillinger(url, queryString)
        return ok(page)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger")
    fun hentMineStillinger(request: HttpServletRequest): ResponseEntity<Page<StillingMedStillingsinfo>> {
        return ok().body(stillingService.hentStillinger(
                 "${externalConfiguration.stillingApi.url}/api/v1/ads/rekrutteringsbistand/minestillinger",
                if (request.queryString != null) URLDecoder.decode(request.queryString, StandardCharsets.UTF_8) else null
        ))
    }

}

private const val replaceRekrutteringsbistandInUrl = "/rekrutteringsbistand"
