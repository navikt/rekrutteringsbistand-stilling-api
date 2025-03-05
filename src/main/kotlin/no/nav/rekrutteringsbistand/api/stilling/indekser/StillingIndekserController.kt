package no.nav.rekrutteringsbistand.api.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/internal/stilling")
@Unprotected
class StillingIndekserController(
    val stillingService: StillingService,
    val rapidApplikasjon: RapidApplikasjon,
    val stillingsinfoService: StillingsinfoService
) {

    @PostMapping("/reindekser")
    fun reindekser(@RequestBody stillingsId: String): ResponseEntity<String> {
        val enStilling = stillingService.hentRekrutteringsbistandStilling(stillingsId, true)
        val packet = JsonMessage.newMessage(eventName = "direktemeldtStillingRepubliser")

        packet["stilling"] = enStilling.stilling
        packet["stillingsinfo"] = stillingsinfoService.hentForStilling(stillingId = Stillingsid(stillingsId)) ?: ""
        packet["stillingId"] = stillingsId

        packet["direktemeldtStilling"] = stillingService.hentDirektemeldtStilling(stillingsId)

        rapidApplikasjon.publish(Stillingsid(stillingsId), packet)

        return ResponseEntity.ok("Stilling $stillingsId er lagt på rapid")
    }

}
