package no.nav.rekrutteringsbistand.api.stillingStatusoppdatering

import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingService
import no.nav.rekrutteringsbistand.api.stilling.Status
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class AktiverOgDeaktiverStillingerService(
    val direktemeldtStillingService: DirektemeldtStillingService
) {

    @Transactional
    fun aktiverStillinger() {
        val stillingerForAktivering = direktemeldtStillingService.hentStillingerForAktivering()
        log.info("Fant ${stillingerForAktivering.size} stillinger for aktivering")

        stillingerForAktivering.forEach {
            val stillingNyStatus = it.copy(status = Status.ACTIVE.toString(), sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
            direktemeldtStillingService.lagreDirektemeldtStilling(stillingNyStatus)
        }
    }

    @Transactional
    fun deaktiverStillinger() {
        val stillingerForDeaktivering  = direktemeldtStillingService.hentStillingerForDeaktivering()
        log.info("Fant ${stillingerForDeaktivering.size} stillinger for deaktivering")
        stillingerForDeaktivering.forEach {
            log.info("Sjekker stilling ${it.stillingsId} med publisert ${it.publisert} og expires ${it.utløpsdato}")

            if(it.adminStatus != Status.DONE.toString()) {
                // Setter administration til done for hvis den ikke er det og har utløpt
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString(),
                    sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
                    adminStatus = Status.DONE.toString())
                direktemeldtStillingService.lagreDirektemeldtStilling(stillingNyStatus)
            } else {
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString(), sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
                direktemeldtStillingService.lagreDirektemeldtStilling(stillingNyStatus)
            }
        }
    }

}
