package no.nav.rekrutteringsbistand.api.konfigurasjon

import no.nav.rekrutteringsbistand.api.requester.HeaderFilter
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
import java.util.Arrays

@Configuration
@EnableAsync
class AppConfig : WebMvcConfigurer {

    @Bean
    fun corsServletFilterRegistration(): FilterRegistrationBean<*> {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.addAllowedOrigin("*")
        config.addAllowedMethod("*")
        config.allowCredentials = true
        config.maxAge = 4000L
        config.addAllowedHeader("*")
        source.registerCorsConfiguration("/**", config)
        val corsFilter = CorsFilter(source)

        val bean = FilterRegistrationBean<CorsFilter>()
        bean.filter = corsFilter
        bean.order = Ordered.HIGHEST_PRECEDENCE
        bean.urlPatterns = setOf("/*")
        bean.isEnabled = true
        return bean
    }

    @Bean
    fun headerFilterRegistration(): FilterRegistrationBean<*> {
        val bean = FilterRegistrationBean<HeaderFilter>()
        bean.setFilter(HeaderFilter())
        bean.setUrlPatterns(Arrays.asList("/rekrutteringsbistand/api/*"))
        bean.setEnabled(true)
        return bean
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

}
