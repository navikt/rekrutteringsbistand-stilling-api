package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.Option
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import org.springframework.stereotype.Service
import java.util.*

@Service
class StillingsinfoService(
    private val   repo: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
) {

    /**
     * Reversering av databaseendringer kan ikke gjøres med @Transactional på REST-endepunktet. Dette skyldes at endringer i databasen
     * må være committa til databasen når kall til rekbis-kandidat-api gjøres. Fordi rekbis-kandidat-api vil gjøre et kall
     * tilbake til rekbis-stilling-api for å hente stillingsinfo, som da altså må være lagra (inkludert committa).
     *
     * Dersom løkka mellom kandidat-api og stilling-api fjernes er manuell håndtering av databasereversering unødvendig.
     * Man kan da putte på en @Transactional på for eksempel endepunktet.
     */
    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: Stillingsid, nyEier: Eier): Stillingsinfo {
        val opprinneligStillingsinfo = repo.hentForStilling(stillingsId).orNull()
        val stillingsinfoMedNyEier = opprinneligStillingsinfo?.copy(eier = nyEier) ?: Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = nyEier
        )
        fun endreEier() {
            opprinneligStillingsinfo?.let { repo.oppdaterEier(it.stillingsinfoid, nyEier) } ?: repo.opprett(stillingsinfoMedNyEier)
        }
        fun reverser() {
            opprinneligStillingsinfo?.let { repo.oppdaterEier(it.stillingsinfoid, opprinneligStillingsinfo.eier) } ?: repo.slett(stillingsId)
        }

        endreEier()
        try {
            kandidatlisteKlient.sendStillingOppdatert(stillingsId)
        } catch (e: Exception) {
            reverser()
            throw RuntimeException("Varsel til rekbis-kandidat-api om endring av eier for ekstern stilling feilet", e)
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId.asString())
        return stillingsinfoMedNyEier
    }

    fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Option<Stillingsinfo> =
        repo.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> =
        repo.hentForStillinger(stillingIder)

    fun hentForIdent(navIdent: String): List<Stillingsinfo> =
        repo.hentForIdent(navIdent)

    fun oppdaterNotat(stillingId: Stillingsid, oppdaterNotat: OppdaterNotat) {
        repo.oppdaterNotat(oppdaterNotat)
    }

    fun lagre(stillingsinfo: Stillingsinfo) {
        repo.opprett(stillingsinfo)
    }

    fun slett(stillingsId: String) {
        repo.slett(stillingsId)
    }

    fun opprettStillingsinfo(stillingsId: Stillingsid, stillingskategori: Stillingskategori) {
        repo.opprett(
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
