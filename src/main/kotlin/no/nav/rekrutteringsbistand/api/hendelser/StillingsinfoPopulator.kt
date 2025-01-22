package no.nav.rekrutteringsbistand.api.hendelser

import com.fasterxml.jackson.annotation.JsonFormat
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
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
            precondition{
                 it.forbid("stillingsinfo")
                 it.forbid("stilling")
                 it.forbidValue("@event_name", "arbeidsgiversKandidatliste.VisningKontaktinfo")
            }
            validate { it.requireKey("stillingsId") }

        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val stillingsId = Stillingsid(packet["stillingsId"].asText())
        log.info("StillingsinfoPopulator er kalt for stillingsid=$stillingsId")
        
        val stillingsinfo = stillingsinfoRepository.hentForStilling(stillingsId) ?: Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = null,
            stillingskategori = null
        ).also { stillingsinfoRepository.opprett(it) }

        packet["stillingsinfo"] = stillingsinfo.tilStillingsinfoIHendelse()

        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.asString())?.also {
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