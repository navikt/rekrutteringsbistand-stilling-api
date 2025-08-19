package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stilling.outbox.EventName
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

@Service
class DirektemeldtStillingService(
    private val direktemeldtStillingRepository: DirektemeldtStillingRepository,
    private val stillingsinfoService: StillingsinfoService,
    private val stillingOutboxService: StillingOutboxService
) {
    fun lagreDirektemeldtStilling(direktemeldtStilling: DirektemeldtStilling) {
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)

        stillingOutboxService.lagreMeldingIOutbox(
            stillingsId = direktemeldtStilling.stillingsId,
            eventName = EventName.INDEKSER_DIREKTEMELDT_STILLING
        )
    }

    fun hentDirektemeldtStilling(stillingsId: String): DirektemeldtStilling? {
        return direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)
    }

    fun hentDirektemeldtStilling(stillingsId: Stillingsid) : DirektemeldtStilling? {
        return hentDirektemeldtStilling(stillingsId.asString())
    }

    fun hentAlleStillingsIder(): List<UUID> {
        return direktemeldtStillingRepository.hentAlleStillingsIder()
    }

    fun hentStillingerForAktivering(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentStillingerForAktivering()
    }

    fun hentStillingerForDeaktivering(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentStillingerForDeaktivering()
    }

    fun hentUtgåtteStillingerFor6mndSidenSomErPending(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentUtgåtteStillingerFor6mndSidenSomErPending()
    }

    @Transactional
    fun overtaEierskapForStillingOgKandidatliste(stillingsinfo: StillingsinfoInboundDto, stilling: DirektemeldtStilling) {
        val oppdatertStilling = stilling.copy(
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            innhold = stilling.innhold.copy(
                administration = stilling.innhold.administration?.copy(
                    navIdent = stillingsinfo.eierNavident,
                    reportee = stillingsinfo.eierNavn
                )
            )
        )
        lagreDirektemeldtStilling(oppdatertStilling)
        val forrigeStillingsinfo = stillingsinfoService.hentForStilling(Stillingsid(stilling.stillingsId))

        if(forrigeStillingsinfo?.stillingsinfoid != null && stillingsinfo.eierNavKontorEnhetId != null) {
            stillingsinfoService.endreNavKontor(stillingsinfoId = forrigeStillingsinfo.stillingsinfoid, navKontorEnhetId = stillingsinfo.eierNavKontorEnhetId)
        }
    }
}
