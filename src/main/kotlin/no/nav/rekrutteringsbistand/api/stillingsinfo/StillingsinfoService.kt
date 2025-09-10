package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteDto
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteStillingDto
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStilling
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class StillingsinfoService(
    private val repo: StillingsinfoRepository,
    private val kandidatlisteKlient: KandidatlisteKlient,
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
    private val stillingssokProxyClient: StillingssokProxyClient,

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
            eier = nyEier,
        )

        opprinneligStillingsinfo?.let { repo.oppdaterEier(it.stillingsinfoid, nyEier) } ?: repo.opprett(stillingsinfoMedNyEier)

        try {
            val arbeidsplassenStilling = stillingssokProxyClient.hentStilling(stillingsId.asString())
            val direktemeldtStillingInnhold = arbeidsplassenStilling.toDirektemeldtStillingInnhold()

            val direktemeldtStilling = DirektemeldtStilling(
                UUID.fromString(stillingsId.toString()),
                direktemeldtStillingInnhold,
                opprettet = arbeidsplassenStilling.created.atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = arbeidsplassenStilling.createdBy,
                sistEndretAv = arbeidsplassenStilling.updatedBy,
                sistEndret = arbeidsplassenStilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                status = arbeidsplassenStilling.status,
                annonsenr = arbeidsplassenStilling.annonsenr,
                utløpsdato = arbeidsplassenStilling.expires?.atZone(ZoneId.of("Europe/Oslo")),
                publisert = arbeidsplassenStilling.published?.atZone(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = arbeidsplassenStilling.publishedByAdmin,
                adminStatus = arbeidsplassenStilling.administration?.status
            )

            val kandidatListeDto = KandidatlisteDto(
                stillingsinfo = stillingsinfoMedNyEier.asStillingsinfoDto(),
                stilling = KandidatlisteStillingDto(direktemeldtStilling)
            )
            kandidatlisteKlient.sendStillingOppdatert(kandidatListeDto)
        } catch (e: Exception) {
            throw RuntimeException("Varsel til rekbis-kandidat-api om endring av eier for ekstern stilling feilet", e)
        }

        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(stillingsId.asString())
        return stillingsinfoMedNyEier
    }

    fun hentStillingsinfo(stillingId: Stillingsid): Stillingsinfo? =
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

    fun endreNavKontor(stillingsinfoId: Stillingsinfoid, navKontorEnhetId: String) {
        repo.oppdaterNavKontorEnhetId(stillingsinfoId, navKontorEnhetId)
    }

    fun opprettStillingsinfo(
        stillingsId: Stillingsid,
        stillingskategori: Stillingskategori,
        eierNavident: String?,
        eierNavn: String?,
        eierNavKontorEnhetId: String?
    ) {
        repo.opprett(
            Stillingsinfo(
                stillingsinfoid = Stillingsinfoid.ny(),
                stillingsid = stillingsId,
                eier = Eier(
                    navident = eierNavident,
                    navn = eierNavn,
                    navKontorEnhetId = eierNavKontorEnhetId
                ),
                stillingskategori = stillingskategori,
            )
        )
    }

}
