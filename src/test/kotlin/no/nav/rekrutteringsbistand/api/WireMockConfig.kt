package no.nav.rekrutteringsbistand.api

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import no.nav.rekrutteringsbistand.api.requester.StillingControllerTest
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("mock")
@Configuration
class WireMockConfig {

    private val LOGGER = LoggerFactory.getLogger(WireMockConfig::class.java)

//    @Bean
//    fun wireMockServer(): WireMockServer {
//        LOGGER.info("Starter Wiremock")
//        return WireMockServer(wireMockConfig().notifier(ConsoleNotifier(true)).port(8189)).apply {
//            stubFor(StillingControllerTest.mappingBuilderStilling())
//            stubFor(StillingControllerTest.mappingBuilderSok())
//            start()
//            LOGGER.info("Wiremock startet ${isRunning} ${port()}")
//        }
//    }
}
