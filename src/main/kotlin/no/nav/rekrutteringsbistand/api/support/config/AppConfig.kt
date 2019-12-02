package no.nav.rekrutteringsbistand.api.support.config

import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.rekrutteringsbistand.api.support.rest.HeaderFilter
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.HttpRequest
import org.springframework.http.client.*
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.filter.CorsFilter
import org.springframework.util.StreamUtils
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
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
        val restTemplate = RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(30000))
                .requestFactory {
                    BufferingClientHttpRequestFactory(HttpComponentsClientHttpRequestFactory(
                            HttpClientBuilder.create()
                                    .setSSLHostnameVerifier(DefaultHostnameVerifier()) // Fix SSL hostname verification for *.local domains
                                    .build()))
                }
                .build()
        restTemplate.messageConverters.add(0, StringHttpMessageConverter(StandardCharsets.UTF_8))
        return restTemplate
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
