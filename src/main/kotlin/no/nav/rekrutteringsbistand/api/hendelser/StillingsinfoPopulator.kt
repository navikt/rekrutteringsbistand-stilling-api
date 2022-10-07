package no.nav.rekrutteringsbistand.api.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository

class StillingsinfoPopulator(
    rapidsConnection: RapidsConnection,
    private val stillingsinfoRepository: StillingsinfoRepository
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandAtFørstkommendeUløsteBehovEr("stilling") }
            validate { it.requireKey("kandidathendelse.stillingsId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId: String = packet["kandidathendelse"]["stillingsId"].asText()
        packet["stilling"] = stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId))
        context.publish(packet.toJson())
    }
}

private fun JsonMessage.demandAtFørstkommendeUløsteBehovEr(informasjonsElement: String) {
    demand("@behov") { behovNode ->
        if (behovNode
                .toList()
                .map(JsonNode::asText)
                .onEach { interestedIn(it) }
                .first { this[it].isMissingOrNull() } != informasjonsElement
        )
            throw Exception("Uinteressant hendelse")
    }
}