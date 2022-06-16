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
    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: String, nyEier: Eier): Stillingsinfo {
        val eksisterendeStillingsinfo = repository.hentForStilling(Stillingsid(stillingsId)).orNull()

        val oppdatertStillingsinfo = if (eksisterendeStillingsinfo != null) {
            oppdaterEier(eksisterendeStillingsinfo, nyEier)
        } else {
            opprettEier(stillingsId, nyEier)
        }

        try {
            kandidatlisteKlient.varsleOmOppdatertStilling(Stillingsid(stillingsId))
        } catch (e: Exception) {
            reverserEierskapsendring(stillingsId, eksisterendeStillingsinfo)
            throw RuntimeException("Varsel til kandidat-api om oppdatert ekstern stilling feilet", e)
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId)
        return oppdatertStillingsinfo
    }

    private fun reverserEierskapsendring(stillingsId: String, opprinneligStillingsinfo: Stillingsinfo?) {
        if (opprinneligStillingsinfo != null) {
            oppdaterEier(opprinneligStillingsinfo, opprinneligStillingsinfo.eier!!)
        } else {
            repository.slett(stillingsId)
        }
    }

    private fun opprettEier(stillingsId: String, eier: Eier): Stillingsinfo {
        val uuid = UUID.randomUUID()
        val stillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(uuid),
            stillingsid = Stillingsid(verdi = stillingsId),
            eier = eier
        )

        repository.opprett(stillingsinfo)

        return stillingsinfo
    }

    private fun oppdaterEier(eksisterendeStillingsinfo: Stillingsinfo, nyEier: Eier): Stillingsinfo {
        val oppdatertStillingsinfo = eksisterendeStillingsinfo.copy(
            eier = nyEier
        )

        repository.oppdaterEierIdentOgEierNavn(
            OppdaterEier(oppdatertStillingsinfo.stillingsinfoid, nyEier)
        )

        return oppdatertStillingsinfo
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
