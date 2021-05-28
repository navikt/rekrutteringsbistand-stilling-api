package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InkluderingTest {

    @Test
    fun `skal sjekke at vi kan prosessere stillingsmeldinger for inkludering`() {
    }


    @TestConfiguration
    class MockInkluderingSpringConfig {

        @Bean
        fun kafkaMockConsumer(): MockConsumer<String, Ad> {
            val topic = TopicPartition(stillingstopic, 0)
            return MockConsumer<String, Ad>(OffsetResetStrategy.EARLIEST).apply {
                schedulePollTask {
                    rebalance(listOf(topic))
                    updateBeginningOffsets(mapOf(Pair(topic, 0)))
                    addRecord(ConsumerRecord(stillingstopic, 0, 0, enAd.uuid.toString(), enAd))
                }
            }
        }
    }

}