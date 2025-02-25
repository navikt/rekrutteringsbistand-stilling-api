package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class AktiverOgDeaktiverStillingerService(
    val direktemeldtStillingRepository: DirektemeldtStillingRepository
) {

    @Transactional
    fun aktiverOgDeaktiverStillinger() {
        val aktiveringskandidater = direktemeldtStillingRepository.hentAktiveringskandidater()

        log.info("Fant ${aktiveringskandidater.size} aktiveringskandidater")

        aktiveringskandidater.forEach {
            val stillingNyStatus = it.copy(status = Status.ACTIVE.toString())
            direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
        }

        val deaktiveringskandidater  = direktemeldtStillingRepository.hentDeaktiveringskandidater()
        log.info("Fant ${deaktiveringskandidater.size} deaktiveringskandidater")
        deaktiveringskandidater.forEach {
            log.info("Sjekker stilling ${it.stillingsid} med publisert ${it.innhold.published} og expires ${it.innhold.expires}")

            if(it.innhold.administration?.status != Status.DONE.toString()) {
                // Setter administration til done for hvis den ikke er det og har utløpt
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString(), innhold = it.innhold.copy(administration = it.innhold.administration?.copy(status = Status.DONE.toString())))
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
            } else {
                val stillingNyStatus = it.copy(status = Status.INACTIVE.toString())
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyStatus)
            }
        }
    }
}
