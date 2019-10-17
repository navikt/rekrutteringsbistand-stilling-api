package no.nav.rekrutteringsbistand.api.konfigurasjon

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "external")
class ExternalConfiguration {
    val stillingApi = StillingApi()

    class StillingApi {
        lateinit var url: String

    }
}




