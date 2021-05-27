package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.MockConsumer
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.test.context.junit4.SpringRunner
import kotlin.concurrent.thread


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InkluderingTest {

    @Test
    fun `skal sjekke at vi kan prosessere stillingsmeldinger for inkludering`() {
        Thread.sleep(3000)
        Thread.sleep(3000)
    }


    @TestConfiguration
    class MockInkluderingSpringConfig {

        @Bean
        fun kafkaMockConsumer(): MockConsumer<String, Ad> {
            val mockConsumer = mockConsumer(false)
            return mockConsumer
        }


        @Bean(destroyMethod = "close")
        fun stillingKafkaConsumer(inkluderingService: InkluderingService, kafkaMockConsumer: MockConsumer<String, Ad>): StillingConsumer {
            val stillingConsumer = StillingConsumer(kafkaMockConsumer, inkluderingService)
            thread { stillingConsumer.start() }
            return stillingConsumer
        }



    }

}