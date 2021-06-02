package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.stilling.ext.avro.Ad
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InkluderingsmuligheterService(private val inkluderingsmuligheterRepository: InkluderingsmuligheterRepository) {

    fun lagreInkluderingsmuligheter(ad: Ad) {
        val inkluderingsmuligheter = ad.toInkluderingsmuligheter()
        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(inkluderingsmuligheter)
    }

    private fun Ad.toInkluderingsmuligheter(): Inkluderingsmulighet {
        val tagsString = this.properties.first { it.key == "tags" }.value.toString()
        val tags: List<String> = ObjectMapper().readValue(tagsString)

        return Inkluderingsmulighet(
            stillingsid = this.uuid.toString(),
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