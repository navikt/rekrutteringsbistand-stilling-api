package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import org.springframework.scheduling.annotation.Scheduled
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service


@Service
class AktiverOgDeaktiverStillingJobb(
    private val aktiverOgDeaktiverStillingerService: AktiverOgDeaktiverStillingerService,
    private val leaderElection: LeaderElection
) {

    @Scheduled(cron = "0 0 * * * *")
    fun aktiverOgDeaktiverStillingerJobb() {
        // Sjekker om det er leader, slik at jobben kun kjører på en pod
        if(leaderElection.isLeader()) {
            log.info("Startet jobb for å aktivere og deaktivere stillinger")
            aktiverOgDeaktiverStillingerService.aktiverOgDeaktiverStillinger()
        }
    }
}

