package no.nav.rekrutteringsbistand.api.support.config

import no.nav.rekrutteringsbistand.api.support.rest.HeaderFilter
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.client.RestTemplate
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
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
        return RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(5000))
                .setReadTimeout(Duration.ofMillis(30000))
                .build()
    }
}
