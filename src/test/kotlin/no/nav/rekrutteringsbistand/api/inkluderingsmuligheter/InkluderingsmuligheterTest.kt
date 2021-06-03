package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.Testdata.enAd
import no.nav.rekrutteringsbistand.api.Testdata.enAdUtenTag
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
import java.util.*


@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InkluderingsmuligheterTest {

    @Autowired
    lateinit var mockConsumer: MockConsumer<String, Ad>

    @Autowired
    lateinit var inkluderingsmuligheterRepository: InkluderingsmuligheterRepository

    companion object {
        var offset: Long = 1
    }

    @Test
    fun `Vi lagrer inkluderingsmuligheter hvis det ikke finnes data`() {
        val stilling = enAd(tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID", "INKLUDERING__FYSISK", "INKLUDERING__ARBEIDSMILJØ", "INKLUDERING__GRUNNLEGGENDE", "TILTAK_ELLER_VIRKEMIDDEL", "TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__MENTORTILSKUDD", "TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS", "PRIORITERT_MÅLGRUPPE", "PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30", "PRIORITERT_MÅLGRUPPE__SENIORER_OVER_45", "PRIORITERT_MÅLGRUPPE__KOMMER_FRA_LAND_UTENFOR_EØS", "PRIORITERT_MÅLGRUPPE__HULL_I_CV_EN", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_UTDANNING", "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_ARBEIDSERFARING", "STATLIG_INKLUDERINGSDUGNAD"]""")
        sendMelding(stilling)

        Thread.sleep(300)

        val lagretInkluderingmulighet = inkluderingsmuligheterRepository.hentSisteInkluderingsmulighet(stilling.uuid.toString())!!

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
    fun `Vi lagrer inkluderingsmuligheter hvis det finnes data men det er en rad og den er tom`() {

        val tomInkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = UUID.randomUUID().toString(),
            radOpprettet = LocalDateTime.now()
        )

        val lagretId = inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(tomInkluderingsmulighet)
        println("Lagret rad med ID $lagretId")
        println("Stilling før Kafka-melding ${inkluderingsmuligheterRepository.hentInkluderingsmulighet(tomInkluderingsmulighet.stillingsid)}")

        val adMedEndretInkluderingsmulighet = enAd(
            stillingsId = tomInkluderingsmulighet.stillingsid,
            tags = """["INKLUDERING__ARBEIDSTID"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet)

        Thread.sleep(1000)

        // hent ut inkludering
        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(adMedEndretInkluderingsmulighet.uuid.toString())
        println(lagretInkluderingsmuligheter)

        // assert på inkludering
        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)
        assertThat(lagretInkluderingsmuligheter.first().tilretteleggingmuligheter).contains("ARBEIDSTID")
        assertThat(lagretInkluderingsmuligheter.last().tilretteleggingmuligheter).isEmpty()
    }

    @Test
    @Ignore
    fun `Vi lagrer inkluderingsmuligheter hvis det finnes data men det er en rad og den er tom, og en eldre rad som ikke er tom`() {

    }

    @Test
    @Ignore
    fun `Vi lagrer inkluderingsmuligheter hvis det finnes data som er annerledes`() {
    }

    @Test
    @Ignore
    fun `Vi lagrer IKKE inkluderingsmuligheter hvis begge er tomme`() {
    }

    @Test
    @Ignore
    fun `Vi lagrer IKKE inkluderingsmuligheter hvis begge er like`() {
    }

    @Test
    fun `To meldinger på Kafka-topic fører til at vi lagrer to rader`() {
        // Send to Kafka-meldinger
        val stillingsId = UUID.randomUUID()
        val stillingV1 = enAd(stillingsId.toString(), tags = "[]")
        val stillingV2 = enAdUtenTag(stillingsId)

        sendMelding(stillingV1)
        Thread.sleep(100)
        sendMelding(stillingV2)

        Thread.sleep(100)

        // Hent ut lagrede rader
        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(stillingV1.uuid.toString())

        // assert at det er to rader
        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)
        assertThat(lagretInkluderingsmuligheter.first().tilretteleggingmuligheter).isEmpty()
        assertThat(lagretInkluderingsmuligheter[1].tilretteleggingmuligheter).isEmpty()
        // assert at hentNyeste henter nyeste
    }

    private fun sendMelding(ad: Ad) {
        println("Sender melding med offset $offset")
        mockConsumer.addRecord(ConsumerRecord(stillingstopic, 0, offset++, ad.uuid.toString(), ad))
    }



}
