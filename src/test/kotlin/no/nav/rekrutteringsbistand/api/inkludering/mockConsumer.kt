package no.nav.rekrutteringsbistand.api.inkludering

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition

val topic = TopicPartition(stillingstopic, 0)

fun mockConsumer(periodiskSendMeldinger: Boolean = true) = MockConsumer<String, Ad>(OffsetResetStrategy.EARLIEST).apply {
    schedulePollTask {
        rebalance(listOf(topic))
        updateBeginningOffsets(mapOf(Pair(topic, 0)))

        if (periodiskSendMeldinger) {
            GlobalScope.launch {
                var offset: Long = 0
                while (!closed()) {
                    addRecord(ConsumerRecord(stillingstopic, 0, offset++, enAd.uuid.toString(), enAd))
                    delay(5_000)
                }
            }
        }
    }
}
