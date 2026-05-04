package no.nav.rekrutteringsbistand.api.stilling.arbeidsplassen

import com.fasterxml.jackson.databind.node.JsonNodeFactory
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
    fun publiserTilRapid(@PathVariable stillingsId: UUID): ResponseEntity<String> {
        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(stillingsId))
        if (stillingsinfo?.stillingskategori == Stillingskategori.FORMIDLING) {
            throw IllegalArgumentException("Kan ikke sende formidling/etterregistrering til arbeidsplassen")
        } else if (stillingsinfo?.stillingskategori == Stillingskategori.JOBBMESSE) {
            throw IllegalArgumentException("Kan ikke sende jobbmesse til arbeidsplassen")
        }

        val stilling = stillingService.hentDirektemeldtStilling(stillingsId)
        val packet = JsonMessage.newMessage(eventName = "publiserEllerAvpubliserTilArbeidsplassen")

        packet["direktemeldtStilling"] = stilling
        packet["stillingsId"] = stillingsId.toString()
        packet["stillingsinfo"] = stillingsinfo?.asStillingsinfoDto() ?: JsonNodeFactory.instance.nullNode()

        rapidApplikasjon.publish(Stillingsid(stillingsId), packet)

        log.info("Publiserte stilling med stillingsId $stillingsId på rapid")

        return ResponseEntity("La stilling på rapid", HttpStatus.OK)
    }
}
