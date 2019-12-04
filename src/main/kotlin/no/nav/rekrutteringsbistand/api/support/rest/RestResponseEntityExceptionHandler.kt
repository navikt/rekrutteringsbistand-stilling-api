package no.nav.rekrutteringsbistand.api.support.rest

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.security.spring.oidc.validation.interceptor.OIDCUnauthorizedException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.io.IOException

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(OIDCUnauthorizedException::class)
    protected fun håndterUinnlogget(e: Exception, webRequest: WebRequest): ResponseEntity<String> {
        val request = (webRequest as ServletWebRequest).request
        val msg = "Unauthorized. requestURI=${request.requestURI}, HTTP method=${request.method}"
        LOG.info(msg, e)
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("You are not authorized to access this resource")
    }

    @ExceptionHandler(IOException::class, ResourceAccessException::class)
    protected fun håndterIOException(e: Exception, webRequest: WebRequest): ResponseEntity<String> {
        val request = (webRequest as ServletWebRequest).request
        val msg = "IO error. requestURI=${request.requestURI}, HTTP method=${request.method}"
        LOG.error(msg, e)
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Connection error")
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    @Deprecated("Bruk arrow.core.Option eller en tom collection istedenfor. Exceptions for kontrollflyt som ikke er feilsituasjoner er et anti-pattern.")
    protected fun handleNoContent(e: RuntimeException, webRequest: WebRequest): ResponseEntity<Any> {
        val uri = (webRequest as ServletWebRequest).request.requestURI
        LOG.info("Ikke innhold: $uri")
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(uri)
    }

    class NoContentException(message: String?) : RuntimeException(message)
}
