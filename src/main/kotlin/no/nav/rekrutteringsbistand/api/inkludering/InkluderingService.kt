package no.nav.rekrutteringsbistand.api.inkludering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.stilling.ext.avro.Ad
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InkluderingService(private val inkluderingRepository: InkluderingRepository) {


    fun lagreInkludering(ad: Ad) {
        val inkluderingsmuligheter = toInkluderingsmuligheter(ad)
        inkluderingRepository.lagreInkludering(inkluderingsmuligheter)
    }

    private fun toInkluderingsmuligheter(ad: Ad): Inkluderingsmulighet {
        val tagstring = ad.properties.first { it.key == "tags" }.value.toString()
        val tags: List<String> = ObjectMapper().readValue(tagstring)

        return Inkluderingsmulighet(
            stillingsid = ad.uuid.toString(),
            tilretteleggingmuligheter = transformerTilNyttFormat(tags, "INKLUDERING"),
            virkemidler = transformerTilNyttFormat(tags, "TILTAK_ELLER_VIRKEMIDDEL"),
            prioriterteMålgrupper = transformerTilNyttFormat(tags, "PRIORITERT_MÅLGRUPPE"),
            statligInkluderingsdugnad = tags.contains("STATLIG_INKLUDERINGSDUGNAD"),
            radOpprettet = LocalDateTime.now()
        )
    }

    private fun transformerTilNyttFormat(tags: List<String>, kategori: String): List<String> {
        return tags
            .filter { it.startsWith(kategori) }
            .map { if (it == kategori) "${it}_KATEGORI" else it.removePrefix("${kategori}__") }
    }
}

data class Inkluderingsmulighet(
    val stillingsid: String,
    val tilretteleggingmuligheter: List<String>,
    val virkemidler: List<String>,
    val prioriterteMålgrupper: List<String>,
    val statligInkluderingsdugnad: Boolean,
    val radOpprettet: LocalDateTime,
)
