package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InkluderingTest {

    @Autowired
    lateinit var mockConsumer: MockConsumer<String, Ad>

    @Autowired
    lateinit var inkluderingRepository: InkluderingRepository

    @Test
    fun `Melding på Kafka-topic fører til at vi lagrer inkluderingsmuligheter i databasen`() {

        val stilling = enAd

        // Trigg melding på Kafka-topic
        sendMelding(stilling)

        // Hent ut inkluderingsmuligheter i databasen
        Thread.sleep(3000)
        val lagretInkluderingmuligheter = inkluderingRepository.hentInkluderingForStillingId(stilling.uuid.toString())
        println(lagretInkluderingmuligheter)
        // Assert at feltene ser riktig ut
//        assertThat(lagretInkluderingmuligheter.tilretteleggingmuligheter).contains("INKLUDERING")
        // TODO: assert resten av verdiene
    }

    @Test
    fun `To meldinger på Kafka-topic fører til at vi lagrer to rader eller skal hente ut nyeste versjon`() {
        TODO()
    }

    @Test
    fun `Skal konvertere tilretteleggingsmuligheter`() {
        TODO()
    }

    private fun sendMelding(ad: Ad) {
        mockConsumer.addRecord(ConsumerRecord(stillingstopic, 0, 0, ad.uuid.toString(), ad))
    }
}
