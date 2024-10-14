package no.nav.rekrutteringsbistand.api

import OpenAiClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class TestApplicationConfig {

    @Bean
    fun openAiClient(): OpenAiClient {
        return OpenAiClient(
            restTemplate = RestTemplate(),
            openAiApiUrl = "http://localhost:9955/openai/deployments/toi-gpt-4o/chat/completions?api-version=2023-03-15-preview",
            openAiApiKey = "test-key"
        )
    }
}