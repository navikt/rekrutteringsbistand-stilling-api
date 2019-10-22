package no.nav.rekrutteringsbistand.api.requester

import no.nav.rekrutteringsbistand.api.LOG
import no.nav.rekrutteringsbistand.api.toMultiValueMap
import org.springframework.http.*
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import javax.servlet.http.HttpServletRequest

/**
 * Base class with common code for proxying requests through API gateway to target endpoints and error handling.
 */
abstract class BaseRestProxyController protected constructor(protected val restTemplate: RestTemplate, protected val targetUrl: String) {

    protected fun proxyJsonRequest(method: HttpMethod,
                                   request: HttpServletRequest,
                                   stripPathPrefix: String,
                                   body: String): ResponseEntity<String> =
            restTemplate.exchange(
                    buildProxyTargetUrl(request, stripPathPrefix),
                    method,
                    HttpEntity(body, proxyHeaders(request)),
                    String::class.java)

    private fun proxyHeaders(request: HttpServletRequest): MultiValueMap<String, String> =
            mapOf(
                    HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON.toString(),
                    HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON.toString()
            ).plus(
                    request.cookies
                            .filter { it.name.startsWith("isso") }
                            .map { HttpHeaders.AUTHORIZATION to "Bearer ${it.value}}" }
            ).toMultiValueMap()

    protected fun buildProxyTargetUrl(request: HttpServletRequest, stripPrefix: String): URI {
        LOG.debug("proxy til url {}", targetUrl)
        return UriComponentsBuilder.fromUriString(targetUrl)
                .path(request.requestURI.substring(stripPrefix.length))
                .replaceQuery(request.queryString)
                .build(true).toUri()
    }

    companion object {
        val ROOT_URL = "/rekrutteringsbistand-api"
    }

}

