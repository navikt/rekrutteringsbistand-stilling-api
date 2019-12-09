package no.nav.rekrutteringsbistand.api.support.featuretoggle

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.FakeUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.util.UnleashConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FeatureToggleConfig {

    companion object {
        const val OPPRETT_KANDIDATLISTE_KNAPP_TOGGLE = "rekrutteringsbistand.opprett-kandidatliste-knapp"
    }

    @Profile("dev", "prod")
    @Bean
    fun unleash(
            byClusterStrategy: ByClusterStrategy?,
            @Value("\${external.unleash.url}") unleashUrl: String,
            @Value("\${spring.profiles.active}") profile: String
    ): Unleash {
        val config = UnleashConfig.builder()
                .appName("rekrutteringsbistand-api")
                .instanceId("rekrutteringsbistand-api-$profile")
                .unleashAPI(unleashUrl)
                .build()
        return DefaultUnleash(
                config,
                byClusterStrategy
        )
    }

    @Profile("local")
    @Bean
    fun unleashMock(): Unleash {
        return FakeUnleash().apply {
            enable(OPPRETT_KANDIDATLISTE_KNAPP_TOGGLE)
        }
    }
}
