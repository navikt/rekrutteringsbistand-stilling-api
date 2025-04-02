package no.nav.rekrutteringsbistand.api.stilling.indekser

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import no.nav.rekrutteringsbistand.api.support.log
import kotlin.concurrent.thread

@RestController
@RequestMapping("/stillinger")
@Protected
class StillingIndekserController(
    val authorizedPartyUtils: AuthorizedPartyUtils,
    val stillingService: StillingService,
    val rapidApplikasjon: RapidApplikasjon,
    val stillingsinfoService: StillingsinfoService
) {

    @GetMapping("/reindekser")
    fun sendStillingerPåRapid(): ResponseEntity<String> {
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
}
