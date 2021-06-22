package no.nav.rekrutteringsbistand.api.config

import no.nav.helse.rapids_rivers.RapidsConnection
import org.mockito.Mockito.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class RapidConfigMock {
    @Bean
    @Profile("default")
    fun rapidConnectionMock():RapidsConnection = mock(RapidsConnection::class.java)
}