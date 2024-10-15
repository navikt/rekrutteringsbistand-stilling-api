package no.nav.rekrutteringsbistand.api.stillingsanalyse

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.rekrutteringsbistand.api.stillingsanalyse.StillingsanalyseController.StillingsanalyseResponsDto
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.secureLog
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
    @Value("\${openai.api.url}") private val openAiApiUrl: String,
    @Value("\${openai.api.key}") private val openAiApiKey: String,
) {
    fun analyserStilling(prompt: String, stillingsId: String): StillingsanalyseResponsDto {
        val headers = HttpHeaders().apply {
            set("api-key", openAiApiKey)
            contentType = MediaType.APPLICATION_JSON
        }

        val requestBody = mapOf(
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.5,
            "max_tokens" to 1500,
        )

        secureLog.info("OpenAI API Request for stilling ${stillingsId}: headers: ${headers} body: ${requestBody} url: ${openAiApiUrl}")

        val entity = HttpEntity(requestBody, headers)

        return try {
            val start = System.currentTimeMillis()
            val response = restTemplate.exchange(
                openAiApiUrl,
                HttpMethod.POST,
                entity,
                String::class.java
            )
            val stop = System.currentTimeMillis()
            secureLog.info("OpenAI API Response for stilling ${stillingsId} (${stop-start}ms: ${response.body}")

            val cleanedResponse = response.body!!.removePrefix("```json").removeSuffix("```").trim()

            val objectMapper = jacksonObjectMapper()
            val openAiResponse = objectMapper.readValue<OpenAiResponse>(cleanedResponse)

            val aiContent = openAiResponse.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Ingen respons fra OpenAI")

            val retur: StillingsanalyseResponsDto = objectMapper.readValue(aiContent)
            log.info("Suksessfult kall mot openAI API for stilling $stillingsId")
            retur

        } catch (ex: Exception) {
            log.error("Feil ved kall til OpenAI API for stilling ${stillingsId}")
            secureLog.error("Feil ved kall til OpenAI API for stilling ${stillingsId}", ex)
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
