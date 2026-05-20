package no.nav.rekrutteringsbistand.api.rekrutteringstreff

import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteDto
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteStillingDto
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.OpprettRekrutteringstreffFormidlingRespons
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.RekrutteringstreffStilling
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@Service
class RekrutteringstreffFormidlingService(
    val geografiService: GeografiService,
    val stillingsinfoService: StillingsinfoService,
    val direktemeldtStillingService: DirektemeldtStillingService,
    val kandidatlisteKlient: KandidatlisteKlient
) {

    @Transactional
    fun opprettRekrutteringsbistandFormidling(
        eierNavIdent: String,
        eierNavn: String,
        eierNavKontorEnhetId: String,
        rekrutteringstreffId: UUID,
        stilling: RekrutteringstreffStilling
    ) : OpprettRekrutteringstreffFormidlingRespons {
        val populertGeografi = geografiService.populerGeografi(stilling.employer.location)
        val opprettetTidspunkt = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        val uuid = UUID.randomUUID()
        val populertLocationList = geografiService.populerLocationList(stilling.locationList)

        val direkemeldtStillingInnhold = DirektemeldtStillingInnhold(
            title = hentTittel(stilling.categoryList),
            administration = DirektemeldtStillingAdministration(
                comments = null,
                reportee = eierNavn,
                remarks = emptyList(),
                navIdent = eierNavIdent,
            ),
            mediaList = emptyList(),
            contactList = emptyList(),
            privacy = "INTERNAL_NOT_SHOWN",
            source = "DIR",
            medium = "DIR",
            reference = uuid.toString(),
            employer = stilling.employer.toDirektemeldtStillingArbeidsgiver().copy(location = populertGeografi),
            location = null,
            locationList = populertLocationList,
            categoryList = stilling.categoryList.map { it.toDirektemeldtStillingKategori() },
            properties = stilling.properties.filterValues { !it.isNullOrBlank() && it != "[]" }, // fjern tomme verdier,
            businessName = stilling.employer.name,
            firstPublished = true,
            deactivatedByExpiry = false,
            activationOnPublishingDate = null
        )

        val direktemeldtStilling = DirektemeldtStilling(
            stillingsId = uuid,
            innhold = direkemeldtStillingInnhold,
            opprettet = opprettetTidspunkt,
            opprettetAv = "pam-rekrutteringsbistand",
            sistEndret = opprettetTidspunkt,
            sistEndretAv = "pam-rekrutteringsbistand",
            status = Status.STOPPED.name,
            annonsenr = uuid.toString(),
            utløpsdato = opprettetTidspunkt,
            publisert = opprettetTidspunkt,
            publisertAvAdmin = opprettetTidspunkt.toString(),
            adminStatus = AdminStatus.DONE.name,
        )
        direktemeldtStillingService.lagreDirektemeldtStilling(direktemeldtStilling)
        direktemeldtStillingService.settAnnonsenrFraDbId(uuid)

        val direktemeldtStillingFraDb = direktemeldtStillingService.hentDirektemeldtStilling(uuid)!!

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = Stillingsid(uuid),
            stillingskategori = Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING,
            eierNavident = eierNavIdent,
            eierNavn = eierNavn,
            eierNavKontorEnhetId = eierNavKontorEnhetId,
            rekrutteringstreffId = rekrutteringstreffId,
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(uuid))

        // Opprett kandidatliste
        val kandidatListeDto = KandidatlisteDto(
            stillingsinfo = stillingsinfo?.asStillingsinfoDto(),
            stilling = KandidatlisteStillingDto(direktemeldtStillingFraDb)
        )
        val respons = kandidatlisteKlient.sendStillingOppdatert(kandidatListeDto)

        val kandidatlisteIdDto = requireNotNull(respons.body) {
            "Mottok tom respons fra kandidat-api ved opprettelse av kandidatliste"
        }
        val kandidatlisteId = requireNotNull(kandidatlisteIdDto.kandidatlisteId) {
            "Mottok respons uten kandidatlisteId fra kandidat-api ved opprettelse av kandidatliste"
        }

        return OpprettRekrutteringstreffFormidlingRespons(
            kandidatlisteId = UUID.fromString(kandidatlisteId),
            stillingsId = direktemeldtStillingFraDb.stillingsId
        )
    }

    private fun hentTittel(kategoriListe: List<Kategori>): String {
        val janzzKategori = kategoriListe.firstOrNull { it.categoryType == "JANZZ" }
        return janzzKategori?.name ?: "Rekrutteringstreff-formidling"
    }
}