package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.*
import org.apache.kafka.clients.consumer.ConsumerRecord
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
