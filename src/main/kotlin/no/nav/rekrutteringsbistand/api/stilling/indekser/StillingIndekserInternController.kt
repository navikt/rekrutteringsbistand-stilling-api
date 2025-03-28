package no.nav.rekrutteringsbistand.api.stilling.indekser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/internal/stilling")
@Unprotected
class StillingIndekserInternController(
    val stillingService: StillingService,
    val rapidApplikasjon: RapidApplikasjon,
    val stillingsinfoService: StillingsinfoService
) {

    @PostMapping("/reindekser")
    fun reindekser(@RequestBody stillingsId: String): ResponseEntity<String> {
        val uuid: UUID
        try {
            uuid = UUID.fromString(stillingsId)
        } catch (e: Exception) {
            return ResponseEntity("Fikk ikke til å konvertere til uuid", HttpStatus.BAD_REQUEST)
        }
        val enStilling = stillingService.hentRekrutteringsbistandStilling(uuid.toString(), true)
        val packet = JsonMessage.newMessage(eventName = "direktemeldtStillingRepubliser")

        packet["stilling"] = enStilling.stilling
        packet["stillingsinfo"] = stillingsinfoService.hentForStilling(stillingId = Stillingsid(uuid))?.asStillingsinfoDto() ?: ""
        packet["stillingsId"] = uuid.toString()

        packet["direktemeldtStilling"] = stillingService.hentDirektemeldtStilling(uuid.toString())

        rapidApplikasjon.publish(Stillingsid(uuid), packet)

        return ResponseEntity.ok("Stilling $uuid er lagt på rapid")
    }
}
