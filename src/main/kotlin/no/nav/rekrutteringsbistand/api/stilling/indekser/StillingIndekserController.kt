package no.nav.rekrutteringsbistand.api.stilling.indekser

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

        val stillinger = stillingService.hentAlleDirektemeldteStillinger()

        stillinger.forEach { stilling ->
            val rekrutteringsbistandStilling = stillingService.hentRekrutteringsbistandStilling(stilling.stillingsId.toString(), true)
            val packet = JsonMessage.newMessage(eventName = "direktemeldtStillingRepubliser")
            packet["stilling"] = rekrutteringsbistandStilling.stilling
            packet["stillingsinfo"] = stillingsinfoService.hentForStilling(stillingId = Stillingsid(rekrutteringsbistandStilling.stilling.uuid)) ?: ""
            packet["stillingsId"] = rekrutteringsbistandStilling.stilling.uuid
            packet["direktemeldtStilling"] = stillingService.hentDirektemeldtStilling(rekrutteringsbistandStilling.stilling.uuid)
            rapidApplikasjon.publish(Stillingsid(rekrutteringsbistandStilling.stilling.uuid), packet)
        }

        return ResponseEntity.ok("Stillinger er lagt på rapid")
    }
}
