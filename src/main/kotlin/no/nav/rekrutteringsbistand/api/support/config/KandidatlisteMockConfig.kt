package no.nav.rekrutteringsbistand.api.support.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@Profile("kandidatlisteMock")
@Configuration
class KandidatlisteMockConfig {

    @Bean(name = ["kandidatlisteWireMock"])
    fun wireMockServer(): WireMockServer {
        return WireMockServer(WireMockConfiguration.wireMockConfig()
                .notifier(ConsoleNotifier(true))
                .port(9924)).apply {
            stubFor(oppdaterKandidatliste())
            start()
            LOG.info("Startet WireMock på port ${port()}")
        }
    }

    companion object {
        fun oppdaterKandidatliste(): MappingBuilder {
            return WireMock.put(WireMock.urlPathMatching("/pam-kandidatsok-api/rest/veileder/stilling/.*/kandidatliste"))
                    .withHeader(HttpHeaders.CONTENT_TYPE, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .withHeader(HttpHeaders.ACCEPT, WireMock.equalTo(MediaType.APPLICATION_JSON.toString()))
                    .willReturn(WireMock.aResponse().withStatus(HttpStatus.NO_CONTENT.value())
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        }


    }
}
