package no.nav.rekrutteringsbistand.api.stillingStatusoppdatering

import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.stilling.Status
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class AktiverOgDeaktiverStillingerService(
    val direktemeldtStillingRepository: DirektemeldtStillingRepository
) {

    @Transactional
    fun aktiverStillinger() {
        val stillingerForAktivering = direktemeldtStillingRepository.hentStillingerForAktivering()
        log.info("Fant ${stillingerForAktivering.size} stillinger for aktivering")

        stillingerForAktivering.forEach {
            val stillingNyStatus = it.copy(status = Status.ACTIVE.toString(), sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
            direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
        }
    }

    @Transactional
    fun deaktiverStillinger() {
        val stillingerForDeaktivering  = direktemeldtStillingRepository.hentStillingerForDeaktivering()
        log.info("Fant ${stillingerForDeaktivering.size} stillinger for deaktivering")
        stillingerForDeaktivering.forEach {
            log.info("Sjekker stilling ${it.stillingsId} med publisert ${it.publisert} og expires ${it.utløpsdato}")

            if(it.innhold.administration?.status != Status.DONE.toString()) {
                // Setter administration til done for hvis den ikke er det og har utløpt
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString(),
                    sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
                    innhold = it.innhold.copy(administration = it.innhold.administration?.copy(status = Status.DONE.toString())))
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
            } else {
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString(), sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
            }
        }
    }

}
