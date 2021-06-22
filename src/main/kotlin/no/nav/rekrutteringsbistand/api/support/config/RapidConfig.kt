package no.nav.rekrutteringsbistand.api.support.config

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class RapidConfig {
    @Bean
    @Profile("!default")
    fun rapidConnection():RapidsConnection = RapidApplication.create(System.getenv())
}