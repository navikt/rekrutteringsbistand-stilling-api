package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InkluderingService(private val inkluderingRepository: InkluderingRepository) {

    fun lagreInkludering(stillinger: List<Ad>) {

        val inkluderingsmuligheter = stillinger.map { toInkluderingsmuligheter(it) }

        inkluderingRepository.lagreInkluderingBatch(inkluderingsmuligheter)
    }


    private fun toInkluderingsmuligheter(ad: Ad): Inkluderingsmuligheter {

    }

}

data class Inkluderingsmuligheter (
    val stillingsid: String,
    val tilretteleggingmuligheter: List<String>,
    val virkemidler: List<String>,
    val prioriterte_maalgrupper: List<String>,
    val statlig_inkluderingsdugnad: Boolean,
    val rad_opprettet: LocalDateTime,
 )
