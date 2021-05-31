package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.springframework.stereotype.Service

@Service
class InkluderingService(private val inkluderingRepository: InkluderingRepository) {

    fun lagreInkludering(stillinger: List<Ad>) {
        val inkluderingsmuligheter = stillinger.map { toInkluderingsmuligheter(it) }
        inkluderingRepository.lagreInkluderingBatch(inkluderingsmuligheter)
    }


    private fun toInkluderingsmuligheter(ad: Ad): Inkluderingsmuligheter {
        val tags: List<String> = ad.properties.any { it.key == "tags" }.toString()
                .removePrefix("[")
                .removeSuffix("]")
                .split(',')


        return Inkluderingsmuligheter(
                stillingsid = ad.uuid.toString(),
                tilretteleggingmuligheter = tags.filter { it.startsWith("INKLUDERING__") },
                virkemidler = tags.filter { it.startsWith("TILTAK_ELLER_VIRKEMIDDEL__") },
                prioriterte_maalgrupper = tags.filter { it.startsWith("PRIORITERT_MÅLGRUPPE__") },
                statlig_inkluderingsdugnad = tags.contains("STATLIG_INKLUDERINGSDUGNAD"),
        )

    }
}

data class Inkluderingsmuligheter(
        val stillingsid: String,
        val tilretteleggingmuligheter: List<String>,
        val virkemidler: List<String>,
        val prioriterte_maalgrupper: List<String>,
        val statlig_inkluderingsdugnad: Boolean
)
