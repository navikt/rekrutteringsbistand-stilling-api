package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "external")
class ExternalConfiguration(val stillingApi: StillingApi = StillingApi()) {
    class StillingApi(var url: String = "")
}
