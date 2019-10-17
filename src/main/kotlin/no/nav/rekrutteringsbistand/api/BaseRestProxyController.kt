package no.nav.rekrutteringsbistand.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.net.URI
import java.util.Arrays

/**
 * Base class with common code for proxying requests through API gateway to target endpoints and error handling.
 */
abstract class BaseRestProxyController protected constructor(protected val restTemplate: RestTemplate, protected val targetUrl: String) {

    protected fun proxyJsonRequest(method: HttpMethod,
                                   request: HttpServletRequest,
                                   stripPathPrefix: String,
                                   body: String): ResponseEntity<String> {
        val targetUrl = buildProxyTargetUrl(request, stripPathPrefix)
        LOG.debug("proxy til url {}", targetUrl.toString())

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON_UTF8
        headers.accept = Arrays.asList(MediaType.APPLICATION_JSON_UTF8)
        request.cookies
                .filter { it.name.startsWith("isso") }
                .map { it.value }
                .firstOrNull()
                .let  { headers.add(HttpHeaders.AUTHORIZATION, "Bearer ${it}}") }

         return restTemplate.exchange(targetUrl, method, HttpEntity(body, headers), String::class.java)
    }

    /**
     * Build target URL based on configured API gateway root URL
     * @param request incoming request
     * @param stripPrefix a path prefix to strip when building target URL
     * @return encoded target service URI with path and query params from request
     */
    protected fun buildProxyTargetUrl(request: HttpServletRequest, stripPrefix: String): URI {
        return UriComponentsBuilder.fromUriString(targetUrl)
                .path(request.requestURI.substring(stripPrefix.length))
                .replaceQuery(request.queryString)
                .build(true).toUri()
    }

    // Directly forward any error responses with no wrapping (better for clients of pam-ad-api)
    @ExceptionHandler(RestClientResponseException::class)
    @Throws(IOException::class)
    protected fun handleResponseException(e: RestClientResponseException): ResponseEntity<String> {
        return ResponseEntity.status(e.rawStatusCode)
                .headers(e.responseHeaders)
                .body(e.responseBodyAsString)
    }

    // Connection errors to targets forwarded back to client as bad gateway errors
    @ExceptionHandler(IOException::class, ResourceAccessException::class)
    @Throws(IOException::class)
    protected fun handleIOException(e: Exception, response: HttpServletResponse) {
        response.sendError(HttpStatus.BAD_GATEWAY.value(), e.message)
    }

    companion object {
        val ROOT_URL = "/rekrutteringsbistand-api"
    }

}

