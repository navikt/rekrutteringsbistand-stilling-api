@file:Suppress("ConfigurationProperties")

// Spring 5.2 trenger ikke lenger @Configuration, men IntelliJ 2019.2 henger ikke helt med.

package no.nav.rekrutteringsbistand.api.konfigurasjon

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "external")
class ExternalConfiguration(val stillingApi: StillingApi = StillingApi()) {
    class StillingApi(var url: String = "")
}




