package no.nav.rekrutteringsbistand.api.config

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.inkludering.stillingstopic
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MockConsumer {
    @Bean
    fun kafkaMockConsumer(): MockConsumer<String, Ad> {
        val topic = TopicPartition(stillingstopic, 0)
        return MockConsumer<String, Ad>(OffsetResetStrategy.EARLIEST).apply {
            schedulePollTask {
                rebalance(listOf(topic))
                updateBeginningOffsets(mapOf(Pair(topic, 0)))
            }
        }
    }
}