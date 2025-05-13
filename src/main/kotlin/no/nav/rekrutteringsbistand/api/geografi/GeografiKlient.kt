package no.nav.rekrutteringsbistand.api.geografi

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

@Component
class GeografiKlient(
    @Value("\${geografi.url}") private val url: String,
) {
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    @Cacheable("postdata")
    fun hentAllePostdata(): List<PostDataDTO> {
        val callId = "rekrutteringsbistand-stilling-api-" + UUID.randomUUID().toString()

        val request = HttpRequest.newBuilder()
            .header("Nav-CallId", callId)
            .uri(URI(url))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() != 200) {
            log.error("Fikk ikke hentet postdata fra pam-geografi. Statuskode: ${response.statusCode()}, body: ${response.body()}")
            return emptyList()
        }

        val postData: List<PostDataDTO> = objectMapper.readValue(response.body(), object : TypeReference<List<PostDataDTO>>() {})

        return postData
    }
}
