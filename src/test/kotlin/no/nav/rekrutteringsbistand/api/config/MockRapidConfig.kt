package no.nav.rekrutteringsbistand.api.config

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MockRapidConfig {
    @Bean
    fun rapidsConnection(): RapidsConnection = TestRapid()
}