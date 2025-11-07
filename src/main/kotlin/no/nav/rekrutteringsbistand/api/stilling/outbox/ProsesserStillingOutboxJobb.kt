package no.nav.rekrutteringsbistand.api.stilling.outbox

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.support.config.LeaderElection
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ProsesserStillingOutboxJobb(
    private val stillingOutboxRepository: StillingOutboxRepository,
    private val direktemeldtStillingService: DirektemeldtStillingService,
    private val stillingsinfoService: StillingsinfoService,
    private val leaderElection: LeaderElection,
    private val rapidApplikasjon: RapidApplikasjon
) {

    @Scheduled(fixedDelay = 10000)
    fun prosesserStillingOutboxJobb() {
        if(leaderElection.isLeader()) {
            val uprosesserteStillinger = stillingOutboxRepository.finnBatchMedUprossesertMeldinger()

            if(uprosesserteStillinger.isNotEmpty()) {
                val stillingsider = uprosesserteStillinger.map { it.stillingsId.toString() }
                log.info("Prosesserer ${uprosesserteStillinger.size} stillinger i outbox. Stillingsider: $stillingsider")

                uprosesserteStillinger.forEach {
                    if(it.eventName == EventName.INDEKSER_STILLINGSINFO) {
                        val stillingsinfo = stillingsinfoService.hentStillingsinfo(stillingId = Stillingsid(it.stillingsId))?.asStillingsinfoDto()

                        val packet = JsonMessage.newMessage(eventName = it.eventName.toString())
                        packet["stillingsinfo"] = stillingsinfo ?: JsonNodeFactory.instance.nullNode()
                        packet["stillingsId"] = it.stillingsId
                        rapidApplikasjon.publish(Stillingsid(it.stillingsId), packet)

                        stillingOutboxRepository.settSomProsessert(it.id)
                    } else {
                        val direktemeldtStilling = direktemeldtStillingService.hentDirektemeldtStilling(it.stillingsId.toString())

                        if(direktemeldtStilling != null) {
                            val stillingsinfo = stillingsinfoService.hentStillingsinfo(stillingId = Stillingsid(direktemeldtStilling.stillingsId))?.asStillingsinfoDto()

                            val packet = JsonMessage.newMessage(eventName = it.eventName.toString())
                            packet["stillingsinfo"] = stillingsinfo ?: JsonNodeFactory.instance.nullNode()
                            packet["stillingsId"] = direktemeldtStilling.stillingsId
                            packet["direktemeldtStilling"] = direktemeldtStilling
                            rapidApplikasjon.publish(Stillingsid(direktemeldtStilling.stillingsId), packet)

                            stillingOutboxRepository.settSomProsessert(it.id)
                            log.info("Sendt stilling ${direktemeldtStilling.stillingsId} på rapid")
                        }
                    }
                }
                log.info("Sendt ${uprosesserteStillinger.size} meldinger på rapid")
            }
        }
    }
}
