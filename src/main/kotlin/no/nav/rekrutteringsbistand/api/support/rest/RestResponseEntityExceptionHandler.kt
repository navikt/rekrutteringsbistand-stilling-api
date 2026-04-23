package no.nav.rekrutteringsbistand.api.support.rest

import jakarta.servlet.http.HttpServletRequest
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpHeaders
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import java.util.Locale
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    override fun handleExceptionInternal(
        ex: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val newBody = when {
            body is ProblemDetail && body !is TraceableProblemDetail -> TraceableProblemDetail.from(body)
            body == null && ex is ErrorResponse -> TraceableProblemDetail.from(ex.updateAndGetBody(null, Locale.getDefault()))
            else -> body
        }
        return super.handleExceptionInternal(ex, newBody, headers, statusCode, request)
    }

    @ExceptionHandler(JwtTokenValidatorException::class)
    fun håndterUinnlogget(e: Exception, request: HttpServletRequest): TraceableProblemDetail {
        log.info("Unauthorized. requestURI=${request.requestURI}, HTTP method=${request.method}", e)
        return TraceableProblemDetail.forStatusAndDetail(
            UNAUTHORIZED, "You are not authorized to access this resource"
        )
    }

    @ExceptionHandler(RestClientResponseException::class)
    fun handleExceptionFraRestTemplate(
        e: RestClientResponseException, request: HttpServletRequest
    ): TraceableProblemDetail {
        val msg = "Mottok HTTP respons ${e.statusCode.value()} fra ${request.method} mot URL ${request.requestURL}"
        when (e.statusCode.value()) {
            403, 404 -> log.info(msg, e)
            else -> log.error(msg, e)
        }
        return TraceableProblemDetail.forStatusAndDetail(e.statusCode, e.responseBodyAsString ?: msg)
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    @Deprecated("Bruk en tom collection istedenfor. Exceptions for kontrollflyt som ikke er feilsituasjoner er et anti-pattern.")
    protected fun handleNoContent(e: RuntimeException, request: HttpServletRequest): ResponseEntity<Any> {
        val uri = request.requestURI
        log.info("No content found at requestURI=${request.requestURI}, HTTP method=${request.method}", e)
        return ResponseEntity.status(NO_CONTENT).body(uri)
    }

    class NoContentException(message: String?) : RuntimeException(message)

}
