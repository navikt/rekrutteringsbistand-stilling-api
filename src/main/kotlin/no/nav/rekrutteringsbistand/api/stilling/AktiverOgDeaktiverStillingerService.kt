package no.nav.rekrutteringsbistand.api.stilling

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
    fun aktiverOgDeaktiverStillinger() {
        // Aktivering av stilinger
        val stillingerForAktivering = direktemeldtStillingRepository.hentStillingerForAktivering()
        log.info("Fant ${stillingerForAktivering.size} stillinger for aktivering")

        stillingerForAktivering.forEach {
            val stillingNyStatus = it.copy(status = Status.ACTIVE.toString(), sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")))
            direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
        }

        // Deaktivering av stillinger
        val stillingerForDeaktivering  = direktemeldtStillingRepository.hentStillingerForDeaktivering()
        log.info("Fant ${stillingerForDeaktivering.size} stillinger for deaktivering")
        stillingerForDeaktivering.forEach {
            log.info("Sjekker stilling ${it.stillingsid} med publisert ${it.innhold.published} og expires ${it.innhold.expires}")

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
