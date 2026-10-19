package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.time.Duration


@Configuration
class AppConfig {

    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder, jsonMapper: JsonMapper): RestTemplate {
        val restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofMinutes(1))
                .build()
        restTemplate.messageConverters.removeIf { it is JacksonJsonHttpMessageConverter }
        restTemplate.messageConverters.add(0, JacksonJsonHttpMessageConverter(jsonMapper))
        restTemplate.messageConverters.add(0, StringHttpMessageConverter(StandardCharsets.UTF_8))
        return restTemplate
    }
}
