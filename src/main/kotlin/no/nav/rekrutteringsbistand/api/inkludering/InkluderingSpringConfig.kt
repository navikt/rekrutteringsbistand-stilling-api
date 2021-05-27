package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("dev", "prod")
class InkluderingSpringConfig {

    @Bean
    fun stillingKafkaConsumer(inkluderingService: InkluderingService) : StillingConsumer {
        val kafkaConsumer = KafkaConsumer<String, Ad>(consumerConfig(1))
        val stillingConsumer = StillingConsumer(kafkaConsumer, inkluderingService)
        stillingConsumer.start()
        return stillingConsumer
    }
}