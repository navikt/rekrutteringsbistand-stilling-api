package no.nav.rekrutteringsbistand.api.opensearch

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

@Component
class StillingssokProxyClient(
    val tokenUtils: TokenUtils,
    @Value("\${stillingssok-proxy.url}") private val stillingssokProxyUrl: String,
    @Value("\${scope.stillingssok-proxy}") private val stillingssokProxyScope: String,
) {
    val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    fun hentStilling(stillingsId: String, somSystembruker: Boolean = false): Stilling {
        val token = if(somSystembruker) {
            tokenUtils.hentSystemToken(stillingssokProxyScope)
        } else {
            tokenUtils.hentOBOToken(stillingssokProxyScope)
        }
        val uuid = try {
            UUID.fromString(stillingsId)
        } catch (e: Exception) {
            log.warn("Feil ved parsing av stillingsId: $stillingsId", e)
            throw IllegalArgumentException("Ugyldig stillingsId: $stillingsId")
        }

        val request = HttpRequest.newBuilder()
            .headers("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .uri(URI("$stillingssokProxyUrl/${URLEncoder.encode(uuid.toString(), Charsets.UTF_8.name())}"))
            .GET().build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        if(response.statusCode() != 200) {
            log.error("Fikk ikke hentet stillingen $stillingsId fra opensearch. Statuskode: ${response.statusCode()}")
            throw RuntimeException("Feil mot rekrutteringsbistand-stillingssok-proxy for å hente stilling $stillingsId")
        }

        val opensSearchResponse = objectMapper.readValue(response.body(), OpensSearchResponse::class.java)

        return opensSearchResponse.toStilling(objectMapper)
    }
}
