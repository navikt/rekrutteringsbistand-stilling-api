package no.nav.rekrutteringsbistand.api.minestillinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class MineStillingerLytter(
    rapidsConnection: RapidsConnection,
    private val mineStillingerService: MineStillingerService
    ): River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.interestedIn("uuid", "adnr", "title", "status", "expires", "updated", "businessName", "source")
            }

        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val erEkstern = packet["source"].asText() != "DIR"

        if (erEkstern) {
            mineStillingerService.behandleMeldingForEksternStilling(
                stillingsId = Stillingsid(packet["uuid"].asText()),
                tittel = packet["title"].asText(),
                status = packet["status"].asText(),
                utløpsdato = packet["expires"].fraLocalDateTimeStringTilZonedDateTime(),
                sistEndret = packet["updated"].fraLocalDateTimeStringTilZonedDateTime(),
                arbeidsgiverNavn = packet["businessName"].asText()
            )
        }
    }
}

private fun JsonNode.fraLocalDateTimeStringTilZonedDateTime() =
    ZonedDateTime.of(LocalDateTime.parse(this.asText()), ZoneId.of("Europe/Oslo"))
