package no.nav.rekrutteringsbistand.api.stilling.arbeidsplassen

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID


@RestController
@RequestMapping("/arbeidsplassen")
@Unprotected
class PubliserTilArbeidsplassenController(
    val stillingService: StillingService,
    val stillingsinfoService: StillingsinfoService,
    val rapidApplikasjon: RapidApplikasjon
) {

    @PostMapping("/publiser/{stillingsId}")
    fun publiserTilRapid(@PathVariable stillingsId: String): ResponseEntity<String> {
        val uuid: UUID
        try {
            uuid = UUID.fromString(stillingsId)
        } catch (e: Exception) {
            return ResponseEntity("Fikk ikke til å konvertere til uuid", HttpStatus.BAD_REQUEST)
        }
        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(uuid))
        if (stillingsinfo?.stillingskategori == Stillingskategori.FORMIDLING) {
            throw IllegalArgumentException("Kan ikke sende formidling/etterregistrering til arbeidsplassen")
        }

        val stilling = stillingService.hentDirektemeldtStilling(uuid.toString())
        val packet = JsonMessage.newMessage(eventName = "publiserEllerAvpubliserTilArbeidsplassen")

        packet["direktemeldtStilling"] = stilling
        packet["stillingsId"] = uuid.toString()

        rapidApplikasjon.publish(Stillingsid(uuid), packet)

        log.info("Publiserte stilling med stillingsId $stillingsId på rapid")

        return ResponseEntity("La stilling på rapid", HttpStatus.OK)
    }
}
