package no.nav.rekrutteringsbistand.api.konfigurasjon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.rekrutteringsbistand.api.LOG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("mock")
@Configuration
class MockConfig {

    @Bean
    fun wireMockServer(): WireMockServer {
        return WireMockServer(wireMockConfig()
                .usingFilesUnderClasspath(".")
                .notifier(ConsoleNotifier(true))
                .port(9014)).apply {
            start()
            LOG.info("Startet WireMock på port ${port()}")
        }
    }
}
