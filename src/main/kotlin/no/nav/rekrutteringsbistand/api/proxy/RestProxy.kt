package no.nav.rekrutteringsbistand.api.proxy

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletRequest

/**
 * Base class with common code for proxying requests through API gateway to target endpoints and error handling.
 */
@Component
class RestProxy(val restTemplate: RestTemplate, val tokenUtils: TokenUtils) {

    fun proxyJsonRequest(method: HttpMethod,
                         request: HttpServletRequest,
                         stripPathPrefix: String,
                         body: String?,
                         targetUrl: String): ResponseEntity<String> {
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
                    CONTENT_TYPE to APPLICATION_JSON_VALUE,
                    ACCEPT to APPLICATION_JSON_VALUE,
                    AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
            ).toMultiValueMap()

    private fun buildProxyTargetUrl(request: HttpServletRequest, stripPrefix: String, targetUrl: String): URI {
        return UriComponentsBuilder.fromUriString(targetUrl)
                .path(request.requestURI.substring(stripPrefix.length))
                .replaceQuery(request.queryString)
                .build(true).toUri()
    }

}

