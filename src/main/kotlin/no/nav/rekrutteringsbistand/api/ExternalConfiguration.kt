package no.nav.rekrutteringsbistand.api

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@ConfigurationProperties(prefix = "external")
class ExternalConfiguration {
    val stillingApi = StillingApi()

    class StillingApi {
        lateinit var url: String

    }
}




