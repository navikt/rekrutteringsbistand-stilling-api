package no.nav.rekrutteringsbistand.api.stilling.indekser

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*
import java.util.*
import kotlin.concurrent.thread

@RestController
@RequestMapping("/reindekser")
class StillingIndekserController(
    val authorizedPartyUtils: AuthorizedPartyUtils,
    val stillingService: StillingService,
    val rapidApplikasjon: RapidApplikasjon,
    val stillingsinfoService: StillingsinfoService
) {

    @PostMapping("/stillinger")
    @Protected
    fun reindekserAlleStillinger(): ResponseEntity<String> {
        if (!authorizedPartyUtils.kallKommerFraStillingIndekser()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        thread {
            val stillinger = stillingService.hentAlleDirektemeldteStillinger()

            stillinger.forEach { stilling ->

                val packet = JsonMessage.newMessage(eventName = "direktemeldtStillingRepubliser")
                val stillingsinfo = stillingsinfoService.hentForStilling(stillingId = Stillingsid(stilling.stillingsid))?.asStillingsinfoDto()

                packet["stillingsinfo"] = stillingsinfo ?: JsonNodeFactory.instance.nullNode()
                packet["stillingsId"] = stilling.stillingsid
                packet["direktemeldtStilling"] = stilling
                rapidApplikasjon.publish(Stillingsid(stilling.stillingsid), packet)
            }
            log.info("${stillinger.size} stillinger er lagt på rapid")
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
        val packet = JsonMessage.newMessage(eventName = "direktemeldtStillingRepubliser")

        val stillingsinfoDto: StillingsinfoDto? = stillingsinfoService.hentForStilling(stillingId = Stillingsid(uuid))?.asStillingsinfoDto()

        packet["stillingsinfo"] = stillingsinfoDto ?: JsonNodeFactory.instance.nullNode()
        packet["stillingsId"] = uuid.toString()
        packet["direktemeldtStilling"] = stillingService.hentDirektemeldtStilling(uuid.toString())

        rapidApplikasjon.publish(Stillingsid(uuid), packet)

        return ResponseEntity.ok("Stilling $uuid er lagt på rapid")
    }
}
