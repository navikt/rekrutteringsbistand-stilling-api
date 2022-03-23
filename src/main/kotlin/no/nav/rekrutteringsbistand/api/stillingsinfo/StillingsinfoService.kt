package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service
import java.util.*

@Service
class StillingsinfoService(private val stillingsinfoRepository: StillingsinfoRepository) {
    fun overtaEierskapForEksternStilling(stillingsId: String, eier: Eier): Stillingsinfo {
        val eksisterendeStillingsinfo = stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId))

        return if (eksisterendeStillingsinfo is Some) {
            oppdaterEier(eksisterendeStillingsinfo.get(), eier)
        } else {
            opprettEier(stillingsId, eier)
        }
    }

    // TODO: Spesifiser i metodenavnet at dette er for ekstern stilling, når vi oppretter en kandidatliste
    fun opprettEier(stillingsId: String, eier: Eier): Stillingsinfo {
        val uuid = UUID.randomUUID()
        val stillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(uuid),
            stillingsid = Stillingsid(verdi = stillingsId),
            eier = eier,
            notat = null,
            stillingskategori = null
        )

        stillingsinfoRepository.opprett(stillingsinfo)

        return stillingsinfo
    }

    fun oppdaterEier(eksisterendeStillingsinfo: Stillingsinfo, nyEier: Eier): Stillingsinfo {
        val oppdatertStillingsinfo = eksisterendeStillingsinfo.copy(
            eier = nyEier
        )

        stillingsinfoRepository.oppdaterEierIdentOgEierNavn(
            OppdaterEier(oppdatertStillingsinfo.stillingsinfoid, nyEier)
        )

        return oppdatertStillingsinfo;
    }

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
        stillingsinfoRepository.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> =
        stillingsinfoRepository.hentForStillinger(stillingIder)

    fun oppdaterNotat(stillingId: Stillingsid, oppdaterNotat: OppdaterNotat) {
        stillingsinfoRepository.oppdaterNotat(oppdaterNotat)
    }

    fun lagre(stillingsinfo: Stillingsinfo) {
        stillingsinfoRepository.opprett(stillingsinfo)
    }

    fun slett(stillingsId: String) {
        stillingsinfoRepository.slett(stillingsId)
    }

    fun opprettStillingsinfo(stillingsId: Stillingsid, stillingskategori: Stillingskategori) {
        stillingsinfoRepository.opprett(
            Stillingsinfo(
                stillingsinfoid = Stillingsinfoid.ny(),
                stillingsid = stillingsId,
                eier = null,
                notat = null,
                stillingskategori = stillingskategori
            )
        )
    }

}
