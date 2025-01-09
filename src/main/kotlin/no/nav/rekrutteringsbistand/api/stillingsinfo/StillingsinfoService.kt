package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class StillingsinfoService(
    private val repo: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient
) {

    @Transactional
    fun overtaEierskapForEksternStillingOgKandidatliste(stillingsId: Stillingsid, nyEier: Eier): Stillingsinfo {
        val opprinneligStillingsinfo = repo.hentForStilling(stillingsId)


        if (opprinneligStillingsinfo?.stillingskategori == Stillingskategori.FORMIDLING) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Kan ikke endre eier på formidlingsstillinger")
        }

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

    fun hentStillingsinfo(stilling: Stilling): Stillingsinfo? =
        hentForStilling(Stillingsid(stilling.uuid))

    fun hentForStilling(stillingId: Stillingsid): Stillingsinfo? =
        repo.hentForStilling(stillingId)

    fun hentForStillinger(stillingIder: List<Stillingsid>): List<Stillingsinfo> {
        val start = System.nanoTime()
        val stillingsinfoListe =  repo.hentForStillinger(stillingIder)
        val tidBruktMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        log.info("Brukte $tidBruktMillis ms på å hente stillingsinfo for ${stillingIder.size} stillinger")
        return stillingsinfoListe
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
                stillingskategori = stillingskategori
            )
        )
    }

}
