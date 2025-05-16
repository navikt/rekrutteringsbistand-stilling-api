package no.nav.rekrutteringsbistand.api.geografi

import org.springframework.stereotype.Service
import no.nav.rekrutteringsbistand.api.support.log

@Service
class GeografiService(
    val geografiKlient: GeografiKlient
) {
    fun finnFylke(kommunenummer: String): String? {
        val postdata = geografiKlient.hentAllePostdata()

        val fylke = postdata.find { it.kommune.kommunenummer == kommunenummer }?.fylke?.navn

        if (fylke == null) {
            log.warn("Fant ikke fylke for $kommunenummer")
        }
        return fylke
    }

    fun finnPostdata(postnummer: String): PostDataDTO? {
        val postdata = geografiKlient.hentAllePostdata()

        val postdataDTO = postdata.find { it.postkode == postnummer }

        if (postdataDTO == null) {
            log.warn("Fant ikke postdata for $postnummer")
        }
        return postdataDTO
    }

    fun finnPostdataFraKommune(kommunenummer: String?, kommuneNavn: String?): PostDataDTO? {
        val postdata = geografiKlient.hentAllePostdata()

        if(!kommunenummer.isNullOrBlank()) {
            return postdata.find { it.kommune.kommunenummer == kommunenummer }
        }

        if(!kommuneNavn.isNullOrBlank()) {
            return postdata.find { it.kommune.navn == kommuneNavn.uppercase() }
        }
        return null
    }
}
