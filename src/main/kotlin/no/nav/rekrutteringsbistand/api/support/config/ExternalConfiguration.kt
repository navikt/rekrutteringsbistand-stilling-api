package no.nav.rekrutteringsbistand.api.support.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "external")
class ExternalConfiguration(
        val pamAdApi: PamAdApi = PamAdApi(),
        val kandidatlisteApi: KandidatlisteApi = KandidatlisteApi()
) {
    class PamAdApi(var url: String = "")
    class KandidatlisteApi(var url: String = "")
}
