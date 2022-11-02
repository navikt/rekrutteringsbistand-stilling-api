package no.nav.rekrutteringsbistand.api.hendelser

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*

class StillingsinfoPopulator(
    rapidsConnection: RapidsConnection,
    private val stillingsinfoRepository: StillingsinfoRepository,
    private val arbeidsplassenKlient: ArbeidsplassenKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("kandidathendelse.stillingsId") }
            validate { it.rejectKey("stillingsinfo") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId: String = packet["kandidathendelse.stillingsId"].asText()
        stillingsinfoRepository.hentForStilling(Stillingsid(stillingsId)).map {
            packet["stillingsinfo"] = it.tilStillingsinfoIHendelse()
        }
        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId).map {
            packet["stillingstittel"] = it.title
        }
        context.publish(packet.toJson())
    }
}

private fun Stillingsinfo.tilStillingsinfoIHendelse() =
    StillingsinfoIHendelse(stillingsinfoid.asString(), stillingsid.asString(), eier, notat, stillingskategori)

private data class StillingsinfoIHendelse(
    val stillingsinfoid: String,
    val stillingsid: String,
    val eier: Eier?,
    val notat: String?,
    val stillingskategori: Stillingskategori?
)
