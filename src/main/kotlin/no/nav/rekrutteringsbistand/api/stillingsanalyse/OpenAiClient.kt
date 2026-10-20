package no.nav.rekrutteringsbistand.api.stillingsanalyse

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import no.nav.rekrutteringsbistand.api.stillingsanalyse.StillingsanalyseController.StillingsanalyseResponsDto
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.rekrutteringsbistand.SecureLog

@Component
class OpenAiClient(
    private val restTemplate: RestTemplate,
    @Value("\${openai.api.url}") private val openAiApiUrl: String,
    @Value("\${openai.api.key}") private val openAiApiKey: String,
) {
    private val secureLog = SecureLog(log)

    fun analyserStilling(stillingsanalyseDto: StillingsanalyseController.StillingsanalyseDto): StillingsanalyseResponsDto {

        val systemMessage = StillingsanalyseTemplate.SYSTEM_MESSAGE
        val userMessage = StillingsanalyseTemplate.lagUserPrompt(stillingsanalyseDto)

        val headers = HttpHeaders().apply {
            set("api-key", openAiApiKey)
            contentType = MediaType.APPLICATION_JSON
        }

        val openAiRequest = OpenAiRequest(
            messages = listOf(
                OpenAiMessage(role = "system", content = systemMessage),
                OpenAiMessage(role = "user", content = userMessage)
            ),
            temperature = 0.5,
            max_tokens = 10000
        )

        log.info("OpenAI API Request for stilling ${stillingsanalyseDto.stillingsId} url: $openAiApiUrl")

        val entity = HttpEntity(openAiRequest, headers)

        return try {
            val start = System.currentTimeMillis()
            val response = restTemplate.exchange(
                openAiApiUrl,
                HttpMethod.POST,
                entity,
                String::class.java
            )

            secureLog.info("svar fra openai: $response")

            val stop = System.currentTimeMillis()
            log.info("OpenAI API Response for stilling ${stillingsanalyseDto.stillingsId} (${stop - start}ms)")

            val objectMapper = jacksonObjectMapper()
            val openAiResponse = objectMapper.readValue<OpenAiResponse>(response.body!!)

            val aiContent = openAiResponse.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Ingen respons fra OpenAI")

            val cleanedResponse = aiContent.removePrefix("```json").removeSuffix("```").trim()

            val retur = objectMapper.readValue<OpenAiStillingsanalyse>(cleanedResponse).tilResponsDto()
            log.info("Suksessfult kall mot OpenAI API for stilling ${stillingsanalyseDto.stillingsId}")
            retur

        } catch (ex: Exception) {
            log.error("Feil ved kall til OpenAI API for stilling ${stillingsanalyseDto.stillingsId}")
            secureLog.error("Feil ved kall til OpenAI API for stilling ${stillingsanalyseDto.stillingsId}", ex)
            throw RuntimeException("Feil ved kall til OpenAI API", ex)
        }
    }
}

data class OpenAiMessage(
    val role: String,
    val content: String
)

/**
 * Svaret fra språkmodellen. De boolske feltene er nullable, og manglende eller null verdier tolkes
 * som false, slik Jackson 2 gjorde. Jackson 3 feiler ellers på manglende primitive verdier.
 */
private data class OpenAiStillingsanalyse(
    val sensitiv: Boolean?,
    val sensitivBegrunnelse: String,
    val samsvarMedTittel: Boolean?,
    val tittelBegrunnelse: String,
    val samsvarMedType: Boolean?,
    val typeBegrunnelse: String
) {
    fun tilResponsDto() = StillingsanalyseResponsDto(
        sensitiv = sensitiv ?: false,
        sensitivBegrunnelse = sensitivBegrunnelse,
        samsvarMedTittel = samsvarMedTittel ?: false,
        tittelBegrunnelse = tittelBegrunnelse,
        samsvarMedType = samsvarMedType ?: false,
        typeBegrunnelse = typeBegrunnelse
    )
}

data class OpenAiRequest(
    val messages: List<OpenAiMessage>,
    val temperature: Double,
    val max_tokens: Int
)


@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiResponse(
    val id: String?,
    val choices: List<Choice>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice(
    val message: OpenAiMessageContent?,
    @JsonProperty("finish_reason") val finishReason: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAiMessageContent(
    val content: String?
)