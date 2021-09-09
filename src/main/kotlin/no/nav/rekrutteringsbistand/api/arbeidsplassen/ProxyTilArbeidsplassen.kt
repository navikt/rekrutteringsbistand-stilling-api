package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.utils.JwtTokenUtil
import org.springframework.http.*
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletRequest

@RestController
@ProtectedWithClaims(issuer = "isso")
class ProxyTilArbeidsplassen(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val tokenUtils: TokenUtils
) {

    @GetMapping("/rekrutteringsbistand/api/v1/geography/municipals")
    fun proxyGetMunicipals(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/geography/counties")
    fun proxyGetCounties(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/geography/countries")
    fun proxyGetCountries(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/categories-with-altnames")
    fun proxyGetCategoriesWithAltnames(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/rekrutteringsbistand/api/v1/postdata")
    fun proxyGetPostdata(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, replaceRekrutteringsbistandInUrl, null, externalConfiguration.stillingApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @GetMapping("/search-api/underenhet/_search")
    private fun getSokTilPamAdApi(request: HttpServletRequest): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.GET, request, "", null, externalConfiguration.sokApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    @PostMapping("/search-api/underenhet/_search")
    private fun postSokTilPamAdApi(request: HttpServletRequest, @RequestBody requestBody: String): ResponseEntity<String> {
        LOG.debug("Mottok ${request.method} til ${request.requestURI}")
        val respons = proxyJsonRequest(HttpMethod.POST, request, "", requestBody, externalConfiguration.sokApi.url)
        return ResponseEntity(respons.body, respons.statusCode)
    }

    private fun proxyJsonRequest(
        method: HttpMethod,
        request: HttpServletRequest,
        stripPathPrefix: String,
        body: String?,
        targetUrl: String
    ): ResponseEntity<String> {
        val url = buildProxyTargetUrl(request, stripPathPrefix, targetUrl)
        LOG.debug("Proxy til URL=$url, HTTP-metode=$method")
        return restTemplate.exchange(
            url,
            method,
            HttpEntity(body, proxyHeaders()),
            String::class.java
        )
    }

    private fun proxyHeaders(): MultiValueMap<String, String> =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
        ).toMultiValueMap()

    private fun buildProxyTargetUrl(request: HttpServletRequest, stripPrefix: String, targetUrl: String): URI {
        return UriComponentsBuilder.fromUriString(targetUrl)
            .path(request.requestURI.substring(stripPrefix.length))
            .replaceQuery(request.queryString)
            .build(true).toUri()
    }
}

private const val replaceRekrutteringsbistandInUrl = "/rekrutteringsbistand"
