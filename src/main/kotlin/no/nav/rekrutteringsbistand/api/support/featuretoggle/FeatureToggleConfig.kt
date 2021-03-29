package no.nav.rekrutteringsbistand.api.support.featuretoggle

import no.finn.unleash.DefaultUnleash
import no.finn.unleash.Unleash
import no.finn.unleash.util.UnleashConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class FeatureToggleConfig {

    @Profile("dev", "prod")
    @Bean
    fun unleash(
            byClusterStrategy: ByClusterStrategy?,
            @Value("\${external.unleash.url}") unleashUrl: String,
            @Value("\${spring.profiles.active}") profile: String
    ): Unleash {
        val config = UnleashConfig.builder()
                .appName("rekrutteringsbistand-stilling-api")
                .instanceId("rekrutteringsbistand-stilling-api-$profile")
                .unleashAPI(unleashUrl)
                .build()
        return DefaultUnleash(
                config,
                byClusterStrategy
        )
    }
}
