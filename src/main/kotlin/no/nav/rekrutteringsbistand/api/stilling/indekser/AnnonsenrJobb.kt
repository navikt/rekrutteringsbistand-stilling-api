package no.nav.rekrutteringsbistand.api.stilling.indekser

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AnnonsenrJobb(
    private val direktemeldtStillingRepository: DirektemeldtStillingRepository,
    private val arbeidsplassenKlient: ArbeidsplassenKlient,
    private val leaderElection: LeaderElection,
) {

    @Scheduled(fixedDelay = 60000) // Kjør hver 60. sekund
    fun settAnnonsenr() {
        if (leaderElection.isLeader()) {
            val stillinger = direktemeldtStillingRepository.hentStillingerUtenAnnonsenr()

            log.info("Skal legge til annonsenr for ${stillinger.size} stillinger")
            stillinger.forEach { stilling ->
                val arbeidsplassenStilling = arbeidsplassenKlient.hentStilling(stilling.stillingsId.toString(), true)
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stilling.copy(annonsenr = arbeidsplassenStilling.id.toString()))
            }
            log.info("Har lagt til ${stillinger.size} annonsenr")
        }
    }
}

