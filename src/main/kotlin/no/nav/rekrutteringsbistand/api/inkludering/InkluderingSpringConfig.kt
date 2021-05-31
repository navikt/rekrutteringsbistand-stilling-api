package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile


@Configuration
class InkluderingSpringConfig {

    @Bean
    @Profile("dev", "prod")
    fun kafkaConsumer(): Consumer<String, Ad> = KafkaConsumer(consumerConfig(1))

}
