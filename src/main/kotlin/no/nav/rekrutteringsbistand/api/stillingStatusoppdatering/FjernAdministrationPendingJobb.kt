package no.nav.rekrutteringsbistand.api.stillingStatusoppdatering

import no.nav.rekrutteringsbistand.api.stilling.AdminStatus
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime


@Service
class FjernAdministrationPendingJobb(
    private val direktemeldtStillingRepository: DirektemeldtStillingRepository,
    private val leaderElection: LeaderElection
) {

    @Scheduled(cron = "0 10 * * * *") // TODO: Endre til kun en gang om dagen etter at jeg har testet ferdig
    fun aktiverOgDeaktiverStillingerJobb() {
        // Sjekker om det er leader, slik at jobben kun kjører på en pod
        if (leaderElection.isLeader()) {
            log.info("Startet jobb for å sette Adminstatus til Done")
            val stillingerSomSkalSettesTilDone = direktemeldtStillingRepository.hentUtgåtteStillingerFor6mndSidenSomErPending()
            log.info("Fant ${stillingerSomSkalSettesTilDone.size} stillinger som skal settes til Done")

            // TODO: Kommenter inn når spørringen over er verifisert
            stillingerSomSkalSettesTilDone.forEach {
                val stillingNyAdminStatus = it.copy(
                    adminStatus = AdminStatus.DONE.toString(),
                    sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
                )
                direktemeldtStillingRepository.lagreDirektemeldtStilling(stillingNyAdminStatus)
                log.info("Satt AdminStatus DONE for ${it.stillingsId} pga eldre en 6mnd")
            }
        }
    }
}
