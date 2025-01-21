package no.nav.rekrutteringsbistand.api.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfoid
import no.nav.rekrutteringsbistand.api.support.log
import org.apache.commons.lang3.math.NumberUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class StillingsinfoPopulatorGammel(
    rapidsConnection: RapidsConnection,
    private val stillingsinfoRepository: StillingsinfoRepository,
    private val arbeidsplassenKlient: ArbeidsplassenKlient
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            precondition{
                it.forbid("stillingsinfo")
            }
            validate { it.requireKey("kandidathendelse.stillingsId") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry
    ) {
        val stillingsId = Stillingsid(packet["kandidathendelse.stillingsId"].asText())
        log.info("StillingsinfoPopulatorGammel er kalt for stillingsid=$stillingsId")

        val stillingsinfo = stillingsinfoRepository.hentForStilling(stillingsId) ?: Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            stillingsid = stillingsId,
            eier = null,
            stillingskategori = null
        ).also { stillingsinfoRepository.opprett(it) }
        packet["stillingsinfo"] = stillingsinfo.tilStillingsinfoIHendelse()

        arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.asString())?.also {
            packet["stilling"] = Stilling(
                stillingstittel = it.title,
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

    private fun isoStringTilNorskTidssone(isoString: String): ZonedDateTime {
        return ZonedDateTime.of(LocalDateTime.parse(isoString), ZoneId.of("Europe/Oslo"))
    }

    private fun parseAntallStillinger(stilling: no.nav.rekrutteringsbistand.api.stilling.Stilling): Int {
        val antallStillinger: String = stilling.properties.getOrDefault("positioncount", "0")
        return if (NumberUtils.isCreatable(antallStillinger)) antallStillinger.toInt() else 0
    }
}
