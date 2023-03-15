package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.Option
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class StillingsinfoService(
    private val   repo: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient
) {

    @Transactional
    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: Stillingsid, nyEier: Eier): Stillingsinfo {
        val opprinneligStillingsinfo = repo.hentForStilling(stillingsId).orNull()
        val stillingsinfoMedNyEier = opprinneligStillingsinfo?.copy(eier = nyEier) ?: Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = nyEier
        )

        opprinneligStillingsinfo?.let { repo.oppdaterEier(it.stillingsinfoid, nyEier) } ?: repo.opprett(stillingsinfoMedNyEier)

        try {
            val stilling = arbeidsplassenKlient.hentStilling(stillingsId.asString(), false)
            val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
                stilling = stilling,
                stillingsinfo = stillingsinfoMedNyEier.asStillingsinfoDto()
            )
            kandidatlisteKlient.sendStillingOppdatert(rekrutteringsbistandStilling)
        } catch (e: Exception) {
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
