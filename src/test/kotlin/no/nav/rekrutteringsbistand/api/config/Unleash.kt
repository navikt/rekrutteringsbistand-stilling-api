package no.nav.rekrutteringsbistand.api.config

import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Unleash {

    @Bean
    fun unleashMock(): Unleash {
        return FakeUnleash().apply {
            enable("rekrutteringsbistand.opprett-kandidatliste-knapp")
        }
    }
}
