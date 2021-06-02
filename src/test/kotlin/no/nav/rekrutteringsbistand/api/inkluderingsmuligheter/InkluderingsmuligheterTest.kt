package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.Testdata.enAd
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.common.PartitionInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.util.*


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InkluderingsmuligheterTest {

    @Autowired
    lateinit var mockConsumer: MockConsumer<String, Ad>

    @Autowired
    lateinit var inkluderingsmuligheterRepository: InkluderingsmuligheterRepository

    var offset: Long = 1

    @Test
    fun `Melding på Kafka-topic fører til at vi lagrer inkluderingsmuligheter i databasen`() {
        val stilling = enAd(tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID", "INKLUDERING__FYSISK", "INKLUDERING__ARBEIDSMILJØ", "INKLUDERING__GRUNNLEGGENDE", "TILTAK_ELLER_VIRKEMIDDEL", "TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__MENTORTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS", "PRIORITERT_MÅLGRUPPE", "PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30", "PRIORITERT_MÅLGRUPPE__SENIORER_OVER_45", "PRIORITERT_MÅLGRUPPE__KOMMER_FRA_LAND_UTENFOR_EØS", "PRIORITERT_MÅLGRUPPE__HULL_I_CV_EN", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_UTDANNING", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_ARBEIDSERFARING", "STATLIG_INKLUDERINGSDUGNAD"]""")
        sendMelding(stilling)

        Thread.sleep(100)

        val lagretInkluderingmulighet: Inkluderingsmulighet = inkluderingsmuligheterRepository.hentInkludering(stilling.uuid.toString()).first()

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

    @Test
    fun `To meldinger på Kafka-topic fører til at vi lagrer to rader`() {
        // Send to Kafka-meldinger
        val stillingsId = UUID.randomUUID()
        val stillingV1 = enAd(stillingsId, tags = """["INKLUDERING"]""")
        val stillingV2 = enAd(stillingsId, tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]""")

        sendMelding(stillingV1)
        sendMelding(stillingV2)

        Thread.sleep(100)

        // Hent ut lagrede rader
        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkludering(stillingV1.uuid.toString())

        // assert at det er to rader
        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)

        // assert at hentNyeste henter nyeste
    }

    private fun sendMelding(ad: Ad) {
        mockConsumer.addRecord(ConsumerRecord(stillingstopic, 0, offset++, ad.uuid.toString(), ad))
    }



}