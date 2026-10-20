package no.nav.rekrutteringsbistand.api.geografi

import tools.jackson.core.type.TypeReference
import no.nav.rekrutteringsbistand.api.support.JacksonMappers
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
    @Value("\${geografi.url}") private val geografiUrl: String,
) {
    val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    @Cacheable("postdata")
    fun hentAllePostdata(): List<PostDataDTO> {
        val url = "$geografiUrl/postdata"
        log.info("Henter all postdata fra url $url")
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

        val postData: List<PostDataDTO> = JacksonMappers.standard.readValue(response.body(), object : TypeReference<List<PostDataDTO>>() {})

        return postData
    }
}
