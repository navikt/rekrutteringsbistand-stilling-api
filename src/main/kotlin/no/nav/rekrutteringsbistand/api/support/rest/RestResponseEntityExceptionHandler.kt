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
    protected fun håndterUinnlogget(): ResponseEntity<String> {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("You are not authorized to access this resource")
    }

    @ExceptionHandler(IOException::class, ResourceAccessException::class)
    protected fun håndterIOException(e: Exception, webRequest: WebRequest): ResponseEntity<String> {
        val uri = (webRequest as ServletWebRequest).request.requestURI
        LOG.error("Håndterer IOException, uri: $uri", e)
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body("Connection error")
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    protected fun handleNoContent(e: RuntimeException, webRequest: WebRequest): ResponseEntity<Any> {
        val uri = (webRequest as ServletWebRequest).request.requestURI
        LOG.error("Ikke innhold: $uri")
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(uri)
    }

    class NoContentException(message: String?) : RuntimeException(message)
}
