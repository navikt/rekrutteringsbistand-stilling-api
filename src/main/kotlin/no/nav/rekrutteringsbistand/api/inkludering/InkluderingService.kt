package no.nav.rekrutteringsbistand.api.inkludering

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.stilling.ext.avro.Ad
import org.springframework.stereotype.Service

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
                tilretteleggingmuligheter = tags.filter { it.startsWith("INKLUDERING") },
                virkemidler = tags.filter { it.startsWith("TILTAK_ELLER_VIRKEMIDDEL") },
                prioriterte_maalgrupper = tags.filter { it.startsWith("PRIORITERT_MÅLGRUPPE") },
                statlig_inkluderingsdugnad = tags.contains("STATLIG_INKLUDERINGSDUGNAD"),
        )

    }
}

data class Inkluderingsmulighet(
        val stillingsid: String,
        val tilretteleggingmuligheter: List<String>,
        val virkemidler: List<String>,
        val prioriterte_maalgrupper: List<String>,
        val statlig_inkluderingsdugnad: Boolean
)
