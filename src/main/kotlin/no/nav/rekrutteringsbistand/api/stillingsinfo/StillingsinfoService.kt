package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service
import java.util.*

@Service
class StillingsinfoService(
    private val stillingsinfoRepository: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
) {

    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: String, eier: Eier): Stillingsinfo {
        val eksisterendeStillingsinfo = stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId)).orNull()

        val oppdatertStillingsinfo = if (eksisterendeStillingsinfo != null) {
            oppdaterEier(eksisterendeStillingsinfo, eier)
        } else {
            opprettEierForEksternStilling(stillingsId, eier)
        }

        try {
            kandidatlisteKlient.varsleOmOppdatertStilling(Stillingsid(stillingsId))
        } catch (e: Exception) {
            if (eksisterendeStillingsinfo != null) {
                oppdaterEier(eksisterendeStillingsinfo, eksisterendeStillingsinfo.eier!!)
            } else {
                stillingsinfoRepository.slett(stillingsId)
            }
            throw RuntimeException("Varsel til kandidat-api om oppdatert ekstern stilling feilet")
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId)
        return oppdatertStillingsinfo
    }

    private fun opprettEierForEksternStilling(stillingsId: String, eier: Eier): Stillingsinfo {
        val uuid = UUID.randomUUID()
        val stillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(uuid),
            stillingsid = Stillingsid(verdi = stillingsId),
            eier = eier
        )

        stillingsinfoRepository.opprett(stillingsinfo)

        return stillingsinfo
    }

    private fun oppdaterEier(eksisterendeStillingsinfo: Stillingsinfo, nyEier: Eier): Stillingsinfo {
        val oppdatertStillingsinfo = eksisterendeStillingsinfo.copy(
            eier = nyEier
        )

        stillingsinfoRepository.oppdaterEierIdentOgEierNavn(
            OppdaterEier(oppdatertStillingsinfo.stillingsinfoid, nyEier)
        )

        return oppdatertStillingsinfo
    }

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
        stillingsinfoRepository.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> =
        stillingsinfoRepository.hentForStillinger(stillingIder)

    fun hentForIdenter(navIdent: String): List<Stillingsinfo> =
        stillingsinfoRepository.hentForIdent(navIdent)

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
