package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.support.LOG
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class InkluderingsmuligheterService(private val inkluderingsmuligheterRepository: InkluderingsmuligheterRepository) {

    fun lagreInkluderingsmuligheter(ad: Ad) {


        val inkluderingsmuligheter = ad.toInkluderingsmuligheter()
        val sisteErLik = sisteErLik(inkluderingsmuligheter)

        LOG.info("Skal behandle ad ${ad.uuid} properties: ${ad.properties}")
        LOG.info("inkluderingsmuligheter for ${ad.uuid}: $inkluderingsmuligheter")
        LOG.info("sisteErLik for ${ad.uuid}: $sisteErLik")

        if (!sisteErLik) {
            inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(inkluderingsmuligheter)
        }
    }

    fun sisteErLik(inkluderingsmuligheter: Inkluderingsmulighet): Boolean {
        val inkluderingsmulighet = inkluderingsmuligheterRepository.hentSisteInkluderingsmulighet(inkluderingsmuligheter.stillingsid)
        return (inkluderingsmulighet == null && !inkluderingsmuligheter.harInkludering()) || (inkluderingsmulighet != null && inkluderingsmuligheter.erLik(inkluderingsmulighet))
    }

    private fun Ad.toInkluderingsmuligheter(): Inkluderingsmulighet {

        val tagStrings = this.properties.filter { it.key == "tags" }
        if (tagStrings.isEmpty()) return Inkluderingsmulighet(
            stillingsid = this.uuid.toString(),
            radOpprettet = LocalDateTime.now()
        )

        val tags: List<String> = ObjectMapper().readValue(tagStrings.first().value.toString())

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
