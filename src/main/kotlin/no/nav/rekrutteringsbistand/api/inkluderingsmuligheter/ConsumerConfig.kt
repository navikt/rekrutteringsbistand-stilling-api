package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.util.*


const val stillingstopic = "arbeidsgiver.rekrutteringsbistand-stilling-1"

@Configuration
class ConsumerConfig {


    @Bean
    @Profile("dev", "prod")
    fun kafkaConsumer(): Consumer<String, Ad> = KafkaConsumer(consumerConfig(versjon = 2))

    private fun consumerConfig(versjon: Int) = Properties().apply {
        put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
        put(ConsumerConfig.GROUP_ID_CONFIG, "rekrutteringsbistand-stilling-til-inkludering-$versjon")
        put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
        put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)

        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BROKERS"))
        put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, System.getenv("KAFKA_SCHEMA_REGISTRY"))
        put(KafkaAvroDeserializerConfig.USER_INFO_CONFIG, "${System.getenv("KAFKA_SCHEMA_REGISTRY_USER")}:${System.getenv("KAFKA_SCHEMA_REGISTRY_PASSWORD")}")
        put(KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO")

        put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)

        put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
        put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
        put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, System.getenv("KAFKA_TRUSTSTORE_PATH"))
        put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("KAFKA_CREDSTORE_PASSWORD"))
        put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
        put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, System.getenv("KAFKA_KEYSTORE_PATH"))
        put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, System.getenv("KAFKA_CREDSTORE_PASSWORD"))
    }
}


