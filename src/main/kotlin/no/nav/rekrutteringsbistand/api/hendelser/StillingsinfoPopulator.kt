package no.nav.rekrutteringsbistand.api.hendelser

import arrow.core.getOrElse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log

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
        val stillingsId = Stillingsid(packet["kandidathendelse.stillingsId"].asText())

        stillingsinfoRepository.hentForStilling(stillingsId).map {
            packet["stillingsinfo"] = it.tilStillingsinfoIHendelse()
        }.getOrElse {
            log.error("Det burde finnes en Stillingsinfo i db for stillingsId=$stillingsId fordi stillingen har en kandidatliste")
            return
        }

        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.asString()).map {
            packet["stilling"] = Stilling(it.title)
        }

        val message: String = packet.toJson()
        context.publish(message)
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

private data class Stilling(
    val stillingstittel: String
)
