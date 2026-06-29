package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature

@Configuration
class JacksonConfig {

    /**
     * Spring Boot 4 bruker Jackson 3, som slår på FAIL_ON_NULL_FOR_PRIMITIVES som standard
     * (motsatt av Jackson 2). Vi slår den av for å beholde tidligere, mer tilgivende oppførsel.
     */
    @Bean
    fun lenientPrimitivesCustomizer() = JsonMapperBuilderCustomizer { builder ->
        builder.disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
    }
}
