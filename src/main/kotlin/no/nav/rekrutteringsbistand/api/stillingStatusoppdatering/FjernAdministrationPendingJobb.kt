package no.nav.rekrutteringsbistand.api.stillingStatusoppdatering

import no.nav.rekrutteringsbistand.api.stilling.AdminStatus
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingService
import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime


@Service
class FjernAdministrationPendingJobb(
    private val direktemeldtStillingService: DirektemeldtStillingService,
    private val leaderElection: LeaderElection
) {

    @Scheduled(cron = "0 30 0 * * *")
    fun fjernAdministrationPendingJobb() {
        // Sjekker om det er leader, slik at jobben kun kjører på en pod
        if (leaderElection.isLeader()) {
            log.info("Startet jobb for å sette Adminstatus til DONE")
            val stillingerSomSkalSettesTilDone = direktemeldtStillingService.hentUtgåtteStillingerFor6mndSidenSomErPending()
            log.info("Fant ${stillingerSomSkalSettesTilDone.size} stillinger som skal settes til AdminStatus DONE")

            stillingerSomSkalSettesTilDone.forEach {
                val stillingNyAdminStatus = it.copy(
                    adminStatus = AdminStatus.DONE.toString(),
                    sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
                )
                direktemeldtStillingService.lagreDirektemeldtStilling(stillingNyAdminStatus)
                log.info("Satt AdminStatus DONE for ${it.stillingsId} pga eldre enn 6mnd")
            }
        }
    }
}
