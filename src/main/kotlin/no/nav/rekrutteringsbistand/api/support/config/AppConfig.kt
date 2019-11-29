package no.nav.rekrutteringsbistand.api.support.config

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.rest.HeaderFilter
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.CorsFilter
import org.springframework.util.StreamUtils
import java.nio.charset.Charset
import java.time.Duration
import java.util.function.Supplier


@Configuration
class AppConfig {

    @Bean
    fun corsServletFilterRegistration(): FilterRegistrationBean<*> =
            FilterRegistrationBean<CorsFilter>().apply {
                filter = CorsFilter(
                        UrlBasedCorsConfigurationSource()
                                .apply {
                                    registerCorsConfiguration("/**", CorsConfiguration().apply {
                                        addAllowedOrigin("*")
                                        addAllowedMethod("*")
                                        allowCredentials = true
                                        maxAge = 4000L
                                        addAllowedHeader("*")
                                    })
                                })
                order = Ordered.HIGHEST_PRECEDENCE
                urlPatterns = setOf("/*")
                isEnabled = true
            }

    @Bean
    fun headerFilterRegistration(): FilterRegistrationBean<*> =
            FilterRegistrationBean<HeaderFilter>().apply {
                setFilter(HeaderFilter())
                urlPatterns = listOf("/rekrutteringsbistand/api/*")
                setEnabled(true)
            }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(30000))
                .interceptors(LogInterceptor())
                .requestFactory { BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory()) }
                .build()
    }

    class LogInterceptor: ClientHttpRequestInterceptor {
        override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
            val response = execution.execute(request, body)
            val responseBody = StreamUtils.copyToString(response.body, Charset.defaultCharset())
            LOG.info("Resttemplate kall. uri: ${request.uri} requestHeaders: ${request.headers}, requestBody: ${String(body)}, responseBody: ${responseBody}, responseHeaders: ${response.headers}")
            return response
        }
    }

    @Bean
    fun httpLogging(): CommonsRequestLoggingFilter? {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(10000)
        filter.setIncludeHeaders(true)
        filter.setAfterMessagePrefix("Request data: ")
        return filter
    }
}
