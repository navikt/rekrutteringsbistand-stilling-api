package no.nav.rekrutteringsbistand.api.stilling.indekser

import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stilling.outbox.EventName
import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import kotlin.concurrent.thread

@RestController
@RequestMapping("/reindekser")
class StillingIndekserController(
    val stillingService: StillingService,
    val stillingOutboxService: StillingOutboxService,
    ) {

    @PostMapping("/stillinger")
    @Unprotected
    fun reindekserAlleStillinger(): ResponseEntity<String> {
        thread {
            val stillingsIder = stillingService.hentAlleStillingsIder()

            stillingsIder.forEach { id ->
                stillingOutboxService.lagreMeldingIOutbox(
                    stillingsId = id,
                    eventName = EventName.REINDEKSER_DIREKTEMELDT_STILLING
                )
            }
            log.info("${stillingsIder.size} stillinger er lagret i outboxen")
        }

        log.info("Mottatt forespørsel om å legge stillinger på rapid")
        return ResponseEntity.ok("Mottatt forespørsel om å legge stillinger på rapid")
    }

    @PostMapping("/stilling/{stillingsId}")
    @Unprotected
    fun reindekser(@PathVariable stillingsId: UUID): ResponseEntity<String> {
        stillingOutboxService.lagreMeldingIOutbox(
            stillingsId = stillingsId,
            eventName = EventName.REINDEKSER_DIREKTEMELDT_STILLING
        )

        return ResponseEntity.ok("Stilling $stillingsId er lagret i outboxen")
    }

}
