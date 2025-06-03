package no.nav.rekrutteringsbistand.api.stilling.indekser

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingRepository
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stilling.outbox.EventName
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
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
    val authorizedPartyUtils: AuthorizedPartyUtils,
    val stillingService: StillingService,
    val stillingOutboxService: StillingOutboxService,
    ) {

    @PostMapping("/stillinger")
    @Unprotected
    fun reindekserAlleStillinger(): ResponseEntity<String> {
        if (!authorizedPartyUtils.kallKommerFraStillingIndekser()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

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
    fun reindekser(@PathVariable stillingsId: String): ResponseEntity<String> {
        val uuid: UUID
        try {
            uuid = UUID.fromString(stillingsId)
        } catch (e: Exception) {
            return ResponseEntity("Fikk ikke til å konvertere til uuid", HttpStatus.BAD_REQUEST)
        }

        stillingOutboxService.lagreMeldingIOutbox(
            stillingsId = uuid,
            eventName = EventName.REINDEKSER_DIREKTEMELDT_STILLING
        )

        return ResponseEntity.ok("Stilling $uuid er lagret i outboxen")
    }

}
