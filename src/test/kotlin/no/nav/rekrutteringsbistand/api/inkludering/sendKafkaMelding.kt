package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer

fun mottaKafkamelding(consumer: MockConsumer<String, Ad>, ad: Ad, offset: Long = 0) {
    val melding = ConsumerRecord(stillingstopic, 0, offset, ad.uuid.toString(), ad)
    consumer.schedulePollTask {
        consumer.addRecord(melding)
    }
}
