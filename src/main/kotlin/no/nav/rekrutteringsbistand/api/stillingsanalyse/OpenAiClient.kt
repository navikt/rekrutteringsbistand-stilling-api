package no.nav.rekrutteringsbistand.api.stillingsanalyse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.rekrutteringsbistand.api.stillingsanalyse.StillingsanalyseController.StillingsanalyseResponsDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class OpenAiClient(
    private val restTemplate: RestTemplate,
    @Value("\${OPENAI_API_URL}") private val openAiApiUrl: String,
    @Value("\${OPENAI_API_KEY}") private val openAiApiKey: String,
) {
    fun analyserStilling(prompt: String, stillingsId: String): StillingsanalyseResponsDto {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $openAiApiKey")
            contentType = MediaType.APPLICATION_JSON
        }

        val requestBody = mapOf(
            "model" to "gpt-4o",
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.5,
            "max_tokens" to 500
        )

        val entity = HttpEntity(requestBody, headers)

        return try {
            val response = restTemplate.exchange(
                openAiApiUrl,
                HttpMethod.POST,
                entity,
                String::class.java
            )
            println("OpenAI API Response: ${response.body}")
            val objectMapper = jacksonObjectMapper()
            val openAiResponse = objectMapper.readValue<OpenAiResponse>(response.body!!)

            val aiContent = openAiResponse.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Ingen respons fra OpenAI")

            objectMapper.readValue(aiContent)
        } catch (ex: Exception) {
            throw RuntimeException("Feil ved kall til OpenAI API", ex)
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OpenAiResponse(
        val id: String? = null,
        val choices: List<AiChoices>? = null
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AiChoices(
        val message: AiMessage? = null,
        @JsonProperty("finish_reason") val finishReason: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class AiMessage(
        val content: String? = null,
    )
}
