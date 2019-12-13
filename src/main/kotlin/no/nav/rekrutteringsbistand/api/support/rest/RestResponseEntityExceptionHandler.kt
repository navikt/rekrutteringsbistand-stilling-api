package no.nav.rekrutteringsbistand.api.support.rest

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.security.spring.oidc.validation.interceptor.OIDCUnauthorizedException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(OIDCUnauthorizedException::class)
    protected fun håndterUinnlogget(e: Exception, request: HttpServletRequest): ResponseEntity<String> {
        val msg = "Unauthorized. requestURI=${request.requestURI}, HTTP method=${request.method}"
        LOG.info(msg, e)
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("You are not authorized to access this resource")
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    @Deprecated("Bruk arrow.core.Option eller en tom collection istedenfor. Exceptions for kontrollflyt som ikke er feilsituasjoner er et anti-pattern.")
    protected fun handleNoContent(e: RuntimeException, request: HttpServletRequest): ResponseEntity<Any> {
        val uri = request.requestURI
        LOG.info("No content found at requestURI=${request.requestURI}, HTTP method=${request.method}")
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(uri)
    }

    class NoContentException(message: String?) : RuntimeException(message)
}
