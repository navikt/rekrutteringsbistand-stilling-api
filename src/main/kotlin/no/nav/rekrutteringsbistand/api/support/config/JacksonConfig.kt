package no.nav.rekrutteringsbistand.api.support.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.ProblemDetail
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.http.converter.json.ProblemDetailJacksonMixin
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Spring Boot 4 bruker Jackson 3 (tools.jackson) som standard i HTTP-laget. Denne konfigurasjonen
 * bytter HTTP-meldingskonverteringen tilbake til Jackson 2 (com.fasterxml) slik at hele
 * applikasjonen bruker samme Jackson-versjon som de interne mapperne.
 */
@Suppress("DEPRECATION")
@Configuration
class JacksonConfig : WebMvcConfigurer {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .addMixIn(ProblemDetail::class.java, ProblemDetailJacksonMixin::class.java)

    @Bean
    fun objectMapper(): ObjectMapper = objectMapper

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        converters.removeIf { it is JacksonJsonHttpMessageConverter }
        converters.add(0, MappingJackson2HttpMessageConverter(objectMapper))
    }
}
