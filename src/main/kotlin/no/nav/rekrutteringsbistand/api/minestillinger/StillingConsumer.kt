package no.nav.rekrutteringsbistand.api.minestillinger

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime


@Component
class StillingConsumer(
    private val mineStillingerService: MineStillingerService
) {

    @KafkaListener(
        topics = ["toi.rekrutteringsbistand-stilling-1"],
        groupId = "rekrutteringsbistand-stilling-api-1",
//        clientIdPrefix = "vedtak-replikert",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun konsumerMelding(melding: ConsumerRecord<String, Ad>) {
        val stilling = melding.value()
        mineStillingerService.behandleMeldingForEksternStilling(
            stillingsId = Stillingsid(stilling.getUuid().toString()),
            tittel = stilling.getTitle().toString(),
            status = stilling.getStatus().name,
            utløpsdato = stilling.getExpires().localDateTimeTilZonedDateTime(),
            sistEndret = stilling.getUpdated().localDateTimeTilZonedDateTime(),
            arbeidsgiverNavn = stilling.getBusinessName().toString()
        )
    }

    private fun CharSequence.localDateTimeTilZonedDateTime() =
        ZonedDateTime.of(LocalDateTime.parse(this.toString()), ZoneId.of("Europe/Oslo"))

}
