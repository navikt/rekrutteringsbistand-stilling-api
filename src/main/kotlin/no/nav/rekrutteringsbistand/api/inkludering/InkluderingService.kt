package no.nav.rekrutteringsbistand.api.inkludering

import org.springframework.stereotype.Service

@Service
class InkluderingService(private val inkluderingRepository: InkluderingRepository) {

    fun lagreInkludering(inkludering: String) {
        inkluderingRepository.lagreInkludering(inkludering)
    }

}