package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import kotlin.concurrent.thread

@Configuration
@Profile("dev", "prod")
class InkluderingSpringConfig {

    @Bean(destroyMethod = "close")
    fun stillingKafkaConsumer(inkluderingService: InkluderingService): StillingConsumer {
        val kafkaConsumer = KafkaConsumer<String, Ad>(consumerConfig(1))
        val stillingConsumer = StillingConsumer(kafkaConsumer, inkluderingService)
        thread { stillingConsumer.start() }
        return stillingConsumer
    }
}

@Configuration
@Profile("!dev", "!prod")
class MockInkluderingSpringConfig {

    @Bean
    fun kafkaMockConsumer(): MockConsumer<String, Ad> {
        val topic = TopicPartition(stillingstopic, 0)
        val mockConsumer = MockConsumer<String, Ad>(OffsetResetStrategy.EARLIEST)
        //mockConsumer.assign(listOf(topic))
        //mockConsumer.rebalance(listOf(topic))
        //mockConsumer.updateBeginningOffsets(mapOf(Pair(topic, 0)))
        return mockConsumer
    }


    @Bean(destroyMethod = "close")
    fun stillingKafkaConsumer(inkluderingService: InkluderingService, kafkaMockConsumer: MockConsumer<String, Ad>): StillingConsumer {
        val stillingConsumer = StillingConsumer(kafkaMockConsumer, inkluderingService)
        thread { stillingConsumer.start() }
        return stillingConsumer
    }
}