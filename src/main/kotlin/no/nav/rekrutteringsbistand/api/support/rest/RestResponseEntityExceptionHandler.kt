package no.nav.rekrutteringsbistand.api.support.rest

import jakarta.servlet.http.HttpServletRequest
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(JwtTokenValidatorException::class)
    protected fun håndterUinnlogget(e: Exception, request: HttpServletRequest): ResponseEntity<String> {
        val msg = "Unauthorized. requestURI=${request.requestURI}, HTTP method=${request.method}"
        log.info(msg, e)
        return ResponseEntity.status(UNAUTHORIZED).body("You are not authorized to access this resource")
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    @Deprecated("Bruk arrow.core.Option eller en tom collection istedenfor. Exceptions for kontrollflyt som ikke er feilsituasjoner er et anti-pattern.")
    protected fun handleNoContent(e: RuntimeException, request: HttpServletRequest): ResponseEntity<Any> {
        val uri = request.requestURI
        log.info("No content found at requestURI=${request.requestURI}, HTTP method=${request.method}", e)
        return ResponseEntity.status(NO_CONTENT).body(uri)
    }

    class NoContentException(message: String?) : RuntimeException(message)

    @ExceptionHandler(value = [RestClientResponseException::class])
    @ResponseBody
    protected fun handleExceptionFraRestTemplate(
        e: RestClientResponseException, request: HttpServletRequest
    ): ResponseEntity<String> {
        val msg = "Mottok HTTP respons ${e.statusCode.value()} fra ${request.method} mot URL ${request.requestURL}"
        when (e.statusCode.value()) {
            403 -> log.info(msg, e)
            404 -> log.info(msg, e)
            else -> log.error(msg, e)
        }
        return ResponseEntity.status(e.statusCode.value()).body(e.responseBodyAsString)
    }
}
