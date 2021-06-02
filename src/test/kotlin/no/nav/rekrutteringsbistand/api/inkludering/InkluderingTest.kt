package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.Testdata.enAd
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime


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
        sendMelding(stilling)

        Thread.sleep(100)

        val lagretInkluderingmulighet: Inkluderingsmulighet = inkluderingRepository.hentInkludering(stilling.uuid.toString()).first()

        assertThat(lagretInkluderingmulighet.stillingsid).isEqualTo(stilling.uuid)
        assertThat(lagretInkluderingmulighet.tilretteleggingmuligheter).containsExactlyInAnyOrder(
            "INKLUDERING_KATEGORI",
            "ARBEIDSTID",
            "FYSISK",
            "ARBEIDSMILJØ",
            "GRUNNLEGGENDE"
        )
        assertThat(lagretInkluderingmulighet.virkemidler).containsExactlyInAnyOrder(
            "TILTAK_ELLER_VIRKEMIDDEL_KATEGORI",
            "LØNNSTILSKUDD",
            "MENTORTILSKUDD",
            "LÆRLINGPLASS"
        )
        assertThat(lagretInkluderingmulighet.prioriterteMålgrupper).containsExactlyInAnyOrder(
            "PRIORITERT_MÅLGRUPPE_KATEGORI",
            "UNGE_UNDER_30",
            "SENIORER_OVER_45",
            "KOMMER_FRA_LAND_UTENFOR_EØS",
            "HULL_I_CV_EN",
            "LITE_ELLER_INGEN_UTDANNING",
            "LITE_ELLER_INGEN_ARBEIDSERFARING"
        )
        assertThat(lagretInkluderingmulighet.statligInkluderingsdugnad).isTrue
        assertThat(lagretInkluderingmulighet.radOpprettet).isBetween(LocalDateTime.now().minusSeconds(1), LocalDateTime.now())
    }

    @Ignore
    @Test
    fun `To meldinger på Kafka-topic fører til at vi lagrer to rader eller skal hente ut nyeste versjon`() {
        TODO()
    }

    @Ignore
    @Test
    fun `Skal konvertere tilretteleggingsmuligheter`() {
        TODO()
    }

    private fun sendMelding(ad: Ad) {
        mockConsumer.addRecord(ConsumerRecord(stillingstopic, 0, 0, ad.uuid.toString(), ad))
    }
}
