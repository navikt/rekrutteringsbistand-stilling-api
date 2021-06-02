package no.nav.rekrutteringsbistand.api.inkludering

import no.nav.pam.stilling.ext.avro.Ad
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class InkluderingTest {

    @Autowired
    lateinit var mockConsumer: MockConsumer<String, Ad>

    @Autowired
    lateinit var inkluderingRepository: InkluderingRepository

    @Test()
    fun `Melding på Kafka-topic fører til at vi lagrer inkluderingsmuligheter i databasen`() {

        val stilling = enAd

        // Trigg melding på Kafka-topic
        sendMelding(stilling)

        // Hent ut inkluderingsmuligheter i databasen
        Thread.sleep(100)
        val lagretInkluderingmulighet: Inkluderingsmulighet = inkluderingRepository.hentInkluderingForStillingId(stilling.uuid.toString()).first()
        assertThat(lagretInkluderingmulighet.stillingsid).isEqualTo(stilling.uuid)

        assertThat(lagretInkluderingmulighet.tilretteleggingmuligheter).containsExactlyInAnyOrder("INKLUDERING", "INKLUDERING__ARBEIDSTID", "INKLUDERING__FYSISK", "INKLUDERING__ARBEIDSMILJØ", "INKLUDERING__GRUNNLEGGENDE")
        assertThat(lagretInkluderingmulighet.virkemidler).containsExactlyInAnyOrder("TILTAK_ELLER_VIRKEMIDDEL", "TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__MENTORTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS")
        assertThat(lagretInkluderingmulighet.prioriterte_maalgrupper).containsExactlyInAnyOrder("PRIORITERT_MÅLGRUPPE", "PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30", "PRIORITERT_MÅLGRUPPE__SENIORER_OVER_45", "PRIORITERT_MÅLGRUPPE__KOMMER_FRA_LAND_UTENFOR_EØS", "PRIORITERT_MÅLGRUPPE__HULL_I_CV_EN", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_UTDANNING", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_ARBEIDSERFARING")
        assertThat(lagretInkluderingmulighet.statlig_inkluderingsdugnad).isTrue

        println(lagretInkluderingmulighet)
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
