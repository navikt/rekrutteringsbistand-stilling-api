package no.nav.rekrutteringsbistand.api.support.rest

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.security.spring.oidc.validation.interceptor.OIDCUnauthorizedException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.util.*
import javax.servlet.http.HttpServletResponse

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(value = [OIDCUnauthorizedException::class, AccessDeniedException::class])
    @ResponseBody
    protected fun handleUnauthorizedException(e: RuntimeException, webRequest: WebRequest): ResponseEntity<Any> {
        return getResponseEntity(e, "You are not authorized to access this ressource", HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(IOException::class, ResourceAccessException::class)
    @Throws(IOException::class)
    protected fun handleIOException(e: Exception, response: HttpServletResponse) {
        LOG.error("IO feil", e)
        response.sendError(HttpStatus.BAD_GATEWAY.value(), e.message)
    }

    @ExceptionHandler(value = [EmptyResultDataAccessException::class, NoContentException::class])
    @ResponseBody
    protected fun handleNoContent(e: RuntimeException, webRequest: WebRequest): ResponseEntity<Any> {
        val uri = (webRequest as ServletWebRequest).request.requestURI
        LOG.error("Ikke innhold: $uri")
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(uri)
    }

    private fun getResponseEntity(e: RuntimeException, melding: String, status: HttpStatus): ResponseEntity<Any> {
        val body = HashMap<String, String>(1)
        body["message"] = melding
        LOG.info(
                String.format(
                        "Returnerer følgende HttpStatus '%s' med melding '%s' pga exception '%s'",
                        status.toString(),
                        melding,
                        e.message
                )
        )

        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body)
    }

    class NoContentException(message: String?): RuntimeException(message)

}
