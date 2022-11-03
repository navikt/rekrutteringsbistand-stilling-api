package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.context.request.RequestContextListener
import java.nio.charset.StandardCharsets
import java.time.Duration
import javax.servlet.annotation.WebListener


@Configuration
class AppConfig {

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
