package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.http.HttpHeaders

@ControllerAdvice
class CustomResponseBodyAdviceAdapter : ResponseBodyAdvice<Any> {

    override fun beforeBodyWrite(o: Any?,
                                 methodParameter: MethodParameter,
                                 mediaType: MediaType,
                                 aClass: Class<out HttpMessageConverter<*>>,
                                 serverHttpRequest: ServerHttpRequest,
                                 serverHttpResponse: ServerHttpResponse): Any? {
        if (serverHttpRequest is ServletServerHttpRequest &&
                serverHttpResponse is ServletServerHttpResponse) {
            LOG.info("advicerestrequest: ${serverHttpRequest.headers} response: ${serverHttpResponse.servletResponse.getHeader(HttpHeaders.CONTENT_TYPE)} o: $o")
        }
        return o
    }

    override fun supports(p0: MethodParameter, p1: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }
}
