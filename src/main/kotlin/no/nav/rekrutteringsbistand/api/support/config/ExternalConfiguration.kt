package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "external")
class ExternalConfiguration(
        val kandidatlisteApi: KandidatlisteApi = KandidatlisteApi()
) {
    class KandidatlisteApi(var url: String = "")
}
