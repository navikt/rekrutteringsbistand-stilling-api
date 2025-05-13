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
}
