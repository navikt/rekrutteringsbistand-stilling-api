package no.nav.rekrutteringsbistand.api.support.config

import no.nav.rekrutteringsbistand.api.support.rest.HeaderFilter
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Duration
import java.util.*


@Configuration
@EnableAsync
class AppConfig : WebMvcConfigurer {

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
}

@Bean
fun headerFilterRegistration(): FilterRegistrationBean<*> =
        FilterRegistrationBean<HeaderFilter>().apply {
            setFilter(HeaderFilter())
            setUrlPatterns(Arrays.asList("/rekrutteringsbistand/api/*"))
            setEnabled(true)
        }

@Bean
fun restTemplateBuilder(): RestTemplateBuilder {
    return RestTemplateBuilder()
            .setConnectTimeout(Duration.ofMillis(5000))
            .setReadTimeout(Duration.ofMillis(30000))
            .requestFactory {
                HttpComponentsClientHttpRequestFactory(
                        HttpClientBuilder.create()
                                .setSSLHostnameVerifier(DefaultHostnameVerifier()) // Fix SSL hostname verification for *.local domains
                                .build())
            }
}
