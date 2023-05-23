package no.nav.rekrutteringsbistand.api.minestillinger;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import no.nav.pam.stilling.ext.avro.Ad;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.Map;

@EnableKafka
@Configuration
public class AivenKafkaAvroConsumerConfig {

    @Value("${KAFKA_BROKERS:http://dummyurl.com:0000}")
    private String brokersUrl;

    @Value("${KAFKA_KEYSTORE_PATH:}")
    private String keystorePath;

    @Value("${KAFKA_TRUSTSTORE_PATH:}")
    private String truststorePath;

    @Value("${KAFKA_CREDSTORE_PASSWORD:}")
    private String credstorePassword;

    @Value("${KAFKA_SCHEMA_REGISTRY:http://dummyurl.com:0000}")
    private String schemaRegistryUrl;

    @Value("${KAFKA_SCHEMA_REGISTRY_USER:}")
    private String schemaRegistryUser;

    @Value("${KAFKA_SCHEMA_REGISTRY_PASSWORD:}")
    private String schemaRegistryPassword;

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, Ad>> kafkaListenerContainerFactory(
            @Qualifier("avroAivenConsumerFactory") ConsumerFactory<String, Ad> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, Ad> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);

        ExponentialBackOff backOff = new ExponentialBackOff(2000, 20);
        backOff.setMaxInterval(172800000);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Ad> avroAivenConsumerFactory(KafkaProperties properties) {
        Map<String, Object> consumerProperties = properties.buildConsumerProperties();

        consumerProperties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        String basicAuth = schemaRegistryUser + ":" + schemaRegistryPassword;
        consumerProperties.put(SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO"); // magic constant 🤯
        consumerProperties.put(SchemaRegistryClientConfig.USER_INFO_CONFIG, basicAuth);

        consumerProperties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        consumerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, brokersUrl);

        if (StringUtils.isNotEmpty(keystorePath)) {
            consumerProperties.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, credstorePassword);
            consumerProperties.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, credstorePassword);
            consumerProperties.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
            consumerProperties.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystorePath);
        }

        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        consumerProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class);
        consumerProperties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        return new DefaultKafkaConsumerFactory<>(consumerProperties);
    }
}
