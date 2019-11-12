package no.nav.rekrutteringsbistand.api.support.rest

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletRequest

/**
 * Base class with common code for proxying requests through API gateway to target endpoints and error handling.
 */
@Component
class RestProxy(restTemplateBuilder: RestTemplateBuilder, val tokenUtils: TokenUtils) {

    var restTemplate = restTemplateBuilder.build()

    fun proxyJsonRequest(method: HttpMethod,
                         request: HttpServletRequest,
                         stripPathPrefix: String,
                         body: String, targetUrl: String): ResponseEntity<String> =
            restTemplate.exchange(
                    buildProxyTargetUrl(request, stripPathPrefix, targetUrl),
                    method,
                    HttpEntity(body, proxyHeaders(request)),
                    String::class.java)

    fun proxyHeaders(request: HttpServletRequest): MultiValueMap<String, String> =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
            ).toMultiValueMap()

    protected fun buildProxyTargetUrl(request: HttpServletRequest, stripPrefix: String, targetUrl: String): URI {
        LOG.debug("proxy til url {}", targetUrl)
        return UriComponentsBuilder.fromUriString(targetUrl)
                .path(request.requestURI.substring(stripPrefix.length))
                .replaceQuery(request.queryString)
                .build(true).toUri()
    }

}

