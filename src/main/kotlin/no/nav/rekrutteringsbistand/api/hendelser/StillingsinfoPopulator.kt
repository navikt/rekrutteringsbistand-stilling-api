package no.nav.rekrutteringsbistand.api.hendelser

import arrow.core.orElse
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
        }.orElse { throw IllegalStateException("Det burde finnes en Stillingsinfo i db for stillingsId=$stillingsId fordi stillingen har en kandidatliste") }

        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.asString()).map {
            packet["stilling"] = Stilling(it.title)
        }
        val message: String = packet.toJson()

        /**
         * Sjekk for å unngå at følgende exception logges men IKKE kastes, og som derfor lar systemet fortsette å konsumere nye meldinger,
         * og det blir feil data sendt til DVH/team Arbeidsmarkedsdata
         *
         * Kibana log message: `Shutting down rapid due to fatal error: The message is 1048751 bytes when serialized which is larger than 1048576, which is the value of the max.request.size configuration.`
         *
         * Kibana stacktrace: `org.apache.kafka.common.errors.RecordTooLargeException: The message is 1048751 bytes when serialized which is larger than 1048576, which is the value of the max.request.size configuration.`
         */
        val kafkaMaxRequestSizeBytes = 1048576
        val messageSizeBytes = message.toByteArray().size
        log.debug("Utgående Kafka-melding med stillingsId=$stillingsId har størrelse i antall bytes: $messageSizeBytes (maksimalt tillatt $kafkaMaxRequestSizeBytes)")
        if (messageSizeBytes > kafkaMaxRequestSizeBytes) {
            log.warn("Utgående Kafka-melding kan være for stor til å bli sendt. stillingsId=$stillingsId, messageSizeBytes=$messageSizeBytes, kafkaMaxRequestSizeBytes=$kafkaMaxRequestSizeBytes")
        }

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
