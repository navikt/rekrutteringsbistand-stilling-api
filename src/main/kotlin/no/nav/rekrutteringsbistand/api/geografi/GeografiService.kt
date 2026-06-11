package no.nav.rekrutteringsbistand.api.geografi

import no.nav.rekrutteringsbistand.api.stilling.Geografi
import org.springframework.stereotype.Service
import no.nav.rekrutteringsbistand.api.support.log

@Service
class GeografiService(
    val geografiKlient: GeografiKlient
) {
    fun finnFylke(fylke: String): String? {
        val postdata = geografiKlient.hentAllePostdata()

        val funnetFylke = postdata.find { it.fylke.navn == fylke.uppercase() }?.fylke?.navn

        if (funnetFylke == null) {
            log.info("Fant ikke fylke for $fylke")
        }
        return funnetFylke
    }

    fun finnPostdata(postnummer: String): PostDataDTO? {
        val postdata = geografiKlient.hentAllePostdata()

        val postdataDTO = postdata.find { it.postkode == postnummer }

        if (postdataDTO == null) {
            log.info("Fant ikke postdata for $postnummer")
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

    fun populerLocationList(locationList: List<Geografi>): List<Geografi> {
        return locationList.mapNotNull { geografi -> populerGeografi(geografi) }
    }

    fun populerGeografi(geografi: Geografi?): Geografi? {
        if(geografi == null) {
            return null
        }

        if (!geografi.postalCode.isNullOrBlank()) {
            val postdata = finnPostdata(geografi.postalCode)
            if (postdata != null) {
                return geografi.copy(
                    municipal = postdata.kommune.navn,
                    county = postdata.fylke.navn,
                    municipalCode = postdata.kommune.kommunenummer,
                    country = "NORGE",
                    city = postdata.by
                )
            }
        }

        val postDataFraKommune = finnPostdataFraKommune(geografi.municipalCode, geografi.municipal)
        if(postDataFraKommune != null) {
            return geografi.copy(
                municipalCode = postDataFraKommune.kommune.kommunenummer,
                municipal = postDataFraKommune.kommune.navn,
                county = postDataFraKommune.fylke.navn,
                country = "NORGE",
            )
        }

        if(!geografi.county.isNullOrBlank()) {
            val fylke = finnFylke(geografi.county)
            if (fylke != null) {
                return geografi.copy(
                    county = fylke,
                    country = "NORGE"
                )
            }
        }
        return geografi
    }
}
