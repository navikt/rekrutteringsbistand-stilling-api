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

    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: Stillingsid, nyEier: Eier): Stillingsinfo {
        val opprinneligStillingsinfo = repository.hentForStilling(stillingsId).orNull()
        val stillingsinfoMedNyEier = opprinneligStillingsinfo?.copy(eier = nyEier) ?: Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = nyEier
        )

        repository.upsert(stillingsinfoMedNyEier)
        try {
            kandidatlisteKlient.sendStillingOppdatert(stillingsId)
        } catch (e: Exception) {
            reverser(opprinneligStillingsinfo, stillingsId)
            throw RuntimeException("Varsel til rekbis-kandidat-api om endring av eier for ekstern stilling feilet", e)
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId.asString())
        return stillingsinfoMedNyEier
    }

    /**
     * Reversering av databaseendringer kan ikke gjøres med @Transactional på REST-endepunktet. Dette skyldes at endringer i databasen
     * må være committa til databasen når kall til rekbis-kandidat-api gjøres. Fordi rekbis-kandidat-api vil gjøre et kall
     * tilbake til rekbis-stilling-api for å hente stillingsinfo, som da altså må være lagra (inkludert committa).
     *
     * Dersom løkka mellom kandidat-api og stilling-api fjernes er manuell håndtering av databasereversering unødvendig.
     * Man kan da putte på en @Transactional på for eksempel endepunktet.
     */
    private fun reverser(opprinneligStillingsinfo: Stillingsinfo?, stillingsId: Stillingsid) {
        opprinneligStillingsinfo?.let { repository.upsert(it) } ?: repository.slett(stillingsId)
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
