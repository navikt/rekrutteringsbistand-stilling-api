package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service
import java.util.*

@Service
class StillingsinfoService(
    private val repository: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
) {

    /**
     * NB: Reversering av databaseendringer kan ikke gjøres med @Transactional. Dette skyldes at endringer i databasen
     * må være "committed" til databasen når kall til kandidat-api gjøres. Dette skyldes at kandidat-api vil gjøre et kall
     * tilbake til stilling-api for å hente stillingsinfo som da må være lagret for at kandidat-api ikke skal kaste feil.
     *
     * Dersom løkka mellom kandidat-api og stilling-api fjernes er manuell håndtering av databasereversering unødvendig.
     * Man kan da putte på en @Transactional på for eksempel endepunktet.
     */
    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: Stillingsid, nyEier: Eier): Stillingsinfo {
        val eksisterendeStillingsinfo = repository.hentForStilling(stillingsId).orNull()
        val stillingHarEier = eksisterendeStillingsinfo?.eier != null

        val oppdatertStillingsinfo = if (stillingHarEier) {
            endreEier(eksisterendeStillingsinfo!!, nyEier)
        } else {
            opprettEierPåEksternStilling(stillingsId, nyEier)
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId.asString())
        return oppdatertStillingsinfo
    }

    private fun endreEier(stillingsinfo: Stillingsinfo, nyEier: Eier): Stillingsinfo {
        repository.oppdaterEier(stillingsinfo.stillingsinfoid, nyEier)

        try {
            kandidatlisteKlient.varsleOmOppdatertStilling(stillingsinfo.stillingsid)
        } catch (e: Exception) {
            repository.oppdaterEier(stillingsinfo, stillingsinfo.eier) // Sette tilbake til forrige eier
            throw RuntimeException("Varsel til kandidat-api om endret eierskap for ekstern stilling feilet", e)
        }

        return stillingsinfo.copy(eier = nyEier)
    }

    private fun opprettEierPåEksternStilling(stillingsId: Stillingsid, eier: Eier): Stillingsinfo {
        val stillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = eier
        )
        repository.opprett(stillingsinfo)

        val varselTilKandidatliste = varsleKandidatlistaOmNyEier(stillingsId)

//        try {
//            kandidatlisteKlient.varsleOmOppdatertStilling(stillingsinfo.stillingsid)
//        } catch (e: Exception) {
//            repository.slett(stillingsId.asString())
//            throw RuntimeException("Varsel til kandidat-api om opprettet eier for ekstern stilling feilet", e)
//        }
        return stillingsinfo
    }

    // Bruke Arrow til å returnere noe gøy?
    private fun varsleKandidatlistaOmNyEier(stillingsId: Stillingsid): Pair<Boolean, Exception?> {
        try {
            kandidatlisteKlient.varsleOmOppdatertStilling(stillingsId)
        } catch (e: Exception) {
            return Pair(false, RuntimeException("Varsel til kandidat-api om oppdatert ekstern stilling feilet", e))
        }
        return Pair(true, null)
    }

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
        repository.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> =
        repository.hentForStillinger(stillingIder)

    fun hentForIdenter(navIdent: String): List<Stillingsinfo> =
        repository.hentForIdent(navIdent)

    fun oppdaterNotat(stillingId: Stillingsid, oppdaterNotat: OppdaterNotat) {
        repository.oppdaterNotat(oppdaterNotat)
    }

    fun lagre(stillingsinfo: Stillingsinfo) {
        repository.opprett(stillingsinfo)
    }

    fun slett(stillingsId: String) {
        repository.slett(stillingsId)
    }

    fun opprettStillingsinfo(stillingsId: Stillingsid, stillingskategori: Stillingskategori) {
        repository.opprett(
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
