package no.nav.rekrutteringsbistand.api.stilling.arbeidsplassen

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/internal/arbeidsplassen")
@Unprotected
class PubliserTilArbeidsplassenController(
    val stillingService: StillingService,
    val rapidApplikasjon: RapidApplikasjon
) {

    @PostMapping("/send")
    fun publiserTilRapid(@RequestBody stillingsId: String): ResponseEntity<String> {
        val uuid: UUID
        try {
            uuid = UUID.fromString(stillingsId)
        } catch (e: Exception) {
            return ResponseEntity("Fikk ikke til å konvertere til uuid", HttpStatus.BAD_REQUEST)
        }

        val stilling = stillingService.hentDirektemeldtStilling(uuid.toString())
        val packet = JsonMessage.newMessage(eventName = "publiserTilArbeidsplassen")

        packet["direktemeldtStilling"] = stilling
        packet["stillingsId"] = uuid.toString()

        rapidApplikasjon.publish(Stillingsid(uuid), packet)

        log.info("Publiserte stilling med stillingsId $stillingsId til rapid")

        return ResponseEntity("La stilling på rapid", HttpStatus.OK)
    }
}
