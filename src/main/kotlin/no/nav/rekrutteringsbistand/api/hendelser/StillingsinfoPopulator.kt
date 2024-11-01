package no.nav.rekrutteringsbistand.api.hendelser

import arrow.core.getOrElse
import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.apache.commons.lang3.math.NumberUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class StillingsinfoPopulator(
    rapidsConnection: RapidsConnection,
    private val stillingsinfoRepository: StillingsinfoRepository,
    private val arbeidsplassenKlient: ArbeidsplassenKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("stillingsId") }
            validate { it.rejectKey("stillingsinfo") }
            validate { it.rejectKey("stilling") }
            validate { it.rejectValue("@event_name", "arbeidsgiversKandidatliste.VisningKontaktinfo") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val stillingsId = Stillingsid(packet["stillingsId"].asText())

        val stillingsinfo = stillingsinfoRepository.hentForStilling(stillingsId).getOrElse {
            val nyStillingsinfo = Stillingsinfo(
                stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
                stillingsid = stillingsId,
                eier = null,
                stillingskategori = null
            )
            stillingsinfoRepository.opprett(nyStillingsinfo)
            nyStillingsinfo
        }
        packet["stillingsinfo"] = stillingsinfo.tilStillingsinfoIHendelse()

        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.asString()).map {
            packet["stilling"] = Stilling(
                stillingstittel = it.hentInternEllerEksternTittel(),
                erDirektemeldt = it.source == "DIR",
                stillingOpprettetTidspunkt = it.publishedByAdmin?.let { tidspunkt -> isoStringTilNorskTidssone(tidspunkt) },
                antallStillinger = parseAntallStillinger(it),
                organisasjonsnummer = it.employer?.orgnr,
                stillingensPubliseringstidspunkt = ZonedDateTime.of(it.published, ZoneId.of("Europe/Oslo"))
            )
        }

        val message: String = packet.toJson()
        context.publish(message)
    }
}


private fun isoStringTilNorskTidssone(isoString: String): ZonedDateTime {
    return ZonedDateTime.of(LocalDateTime.parse(isoString), ZoneId.of("Europe/Oslo"))
}

private fun parseAntallStillinger(stilling: no.nav.rekrutteringsbistand.api.stilling.Stilling): Int {
    val antallStillinger: String = stilling.properties.getOrDefault("positioncount", "0")
    return if (NumberUtils.isCreatable(antallStillinger)) antallStillinger.toInt() else 0
}

fun Stillingsinfo.tilStillingsinfoIHendelse() =
    StillingsinfoIHendelse(stillingsinfoid.asString(), stillingsid.asString(), eier, null, stillingskategori)

data class StillingsinfoIHendelse(
    val stillingsinfoid: String,
    val stillingsid: String,
    val eier: Eier?,
    val notat: String?,
    val stillingskategori: Stillingskategori?
)

data class Stilling(
    val stillingstittel: String,
    val erDirektemeldt: Boolean,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = zonedDateTimeFormat)
    val stillingOpprettetTidspunkt: ZonedDateTime?,
    val antallStillinger: Int,
    val organisasjonsnummer: String?,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = zonedDateTimeFormat)
    val stillingensPubliseringstidspunkt: ZonedDateTime
)

const val zonedDateTimeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX'['VV']'"