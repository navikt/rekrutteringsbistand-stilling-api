package no.nav.rekrutteringsbistand.api.hendelser

import com.fasterxml.jackson.annotation.JsonFormat
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.rekrutteringsbistand.api.stilling.StillingService
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.apache.commons.lang3.math.NumberUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class StillingPopulator(
    rapidsConnection: RapidsConnection,
    private val stillingService: StillingService
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.forbid("stillingsinfo")
                it.forbid("stilling")
                it.forbid("direktemeldtStilling")
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

        log.info("Populerer melding med stilling og stillingsinfo for stillingsId=${stillingsId.asString()}")

        val rekrutteringsbistandStilling = stillingService.hentRekrutteringsbistandStilling(stillingsId.asString(), somSystembruker = true)

        rekrutteringsbistandStilling.stillingsinfo?.also {
            packet["stillingsinfo"] = it.tilStillingsinfoIHendelse()
        }

        packet["stilling"] = Stilling(
            stillingstittel = rekrutteringsbistandStilling.stilling.hentInternEllerEksternTittel(),
            erDirektemeldt = rekrutteringsbistandStilling.stilling.source == "DIR",
            stillingOpprettetTidspunkt = rekrutteringsbistandStilling.stilling.publishedByAdmin?.let { tidspunkt -> isoStringTilNorskTidssone(tidspunkt) },
            antallStillinger = parseAntallStillinger(rekrutteringsbistandStilling.stilling),
            organisasjonsnummer = rekrutteringsbistandStilling.stilling.employer?.orgnr,
            stillingensPubliseringstidspunkt = ZonedDateTime.of(rekrutteringsbistandStilling.stilling.published, ZoneId.of("Europe/Oslo"))
        )

        val message: String = packet.toJson()

        context.publish(message)
    }
}


private fun isoStringTilNorskTidssone(isoString: String): ZonedDateTime? {
    return try {
        ZonedDateTime.of(LocalDateTime.parse(isoString), ZoneId.of("Europe/Oslo"))
    } catch (e: Exception) {
        null;
    }
}

private fun parseAntallStillinger(stilling: no.nav.rekrutteringsbistand.api.stilling.FrontendStilling): Int {
    val antallStillinger: String = stilling.properties.getOrDefault("positioncount", "0")
    return if (NumberUtils.isCreatable(antallStillinger)) antallStillinger.toInt() else 0
}

fun StillingsinfoDto.tilStillingsinfoIHendelse() =
    StillingsinfoIHendelse(stillingsinfoid, stillingsid, Eier(eierNavident, eierNavn, eierNavKontorEnhetId), stillingskategori)

data class StillingsinfoIHendelse(
    val stillingsinfoid: String,
    val stillingsid: String,
    val eier: Eier?,
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
