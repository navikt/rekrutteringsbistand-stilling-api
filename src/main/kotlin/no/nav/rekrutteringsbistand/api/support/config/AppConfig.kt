package no.nav.rekrutteringsbistand.api.support.config

import no.nav.rekrutteringsbistand.api.support.rest.HeaderFilter
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.time.Duration


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
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(1))
                .build()
        restTemplate.messageConverters.add(0, StringHttpMessageConverter(StandardCharsets.UTF_8))
        return restTemplate
    }
}
