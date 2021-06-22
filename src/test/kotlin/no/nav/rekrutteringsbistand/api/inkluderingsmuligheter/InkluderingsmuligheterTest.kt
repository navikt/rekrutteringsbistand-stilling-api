package no.nav.rekrutteringsbistand.api.inkluderingsmuligheter

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.rekrutteringsbistand.api.Testdata.enAd
import no.nav.rekrutteringsbistand.api.Testdata.enAdUtenTag
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.assertj.core.api.Assertions.assertThat
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
    @Test
    fun `ikke i bruk for nå`(){}
/*
    @Autowired
    lateinit var mockConsumer: MockConsumer<String, Ad>

    @Autowired
    lateinit var inkluderingsmuligheterRepository: InkluderingsmuligheterRepository

    companion object {
        var offset: Long = 1
    }

    @Test
    fun `Vi lagrer inkluderingsmuligheter hvis det ikke finnes data`() {
        val stilling = enAd(tags =
            """["INKLUDERING",
                "INKLUDERING__ARBEIDSTID",
                "INKLUDERING__FYSISK",
                "INKLUDERING__ARBEIDSMILJØ",
                "INKLUDERING__GRUNNLEGGENDE",
                "TILTAK_ELLER_VIRKEMIDDEL",
                "TILTAK_ELLER_VIRKEMIDDEL__LØNNSTILSKUDD",
                "TILTAK_ELLER_VIRKEMIDDEL__MENTORTILSKUDD",
                "TILTAK_ELLER_VIRKEMIDDEL__LÆRLINGPLASS",
                "PRIORITERT_MÅLGRUPPE",
                "PRIORITERT_MÅLGRUPPE__UNGE_UNDER_30",
                "PRIORITERT_MÅLGRUPPE__SENIORER_OVER_45",
                "PRIORITERT_MÅLGRUPPE__KOMMER_FRA_LAND_UTENFOR_EØS",
                "PRIORITERT_MÅLGRUPPE__HULL_I_CV_EN",
                "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_UTDANNING",
                "PRIORITERT_MÅLGRUPPE__LITE_ELLER_INGEN_ARBEIDSERFARING",
                "STATLIG_INKLUDERINGSDUGNAD"]"""
        )

        sendMelding(stilling)

        ventLitt()

        val lagretInkluderingmulighet = inkluderingsmuligheterRepository.hentSisteInkluderingsmulighet(stilling.uuid.toString())!!

        assertThat(lagretInkluderingmulighet.stillingsid).isEqualTo(stilling.uuid)
        assertThat(lagretInkluderingmulighet.tilretteleggingmuligheter).containsExactlyInAnyOrder(
            "ARBEIDSTID",
            "FYSISK",
            "ARBEIDSMILJØ",
            "GRUNNLEGGENDE"
        )
        assertThat(lagretInkluderingmulighet.virkemidler).containsExactlyInAnyOrder(
            "LØNNSTILSKUDD",
            "MENTORTILSKUDD",
            "LÆRLINGPLASS"
        )
        assertThat(lagretInkluderingmulighet.prioriterteMålgrupper).containsExactlyInAnyOrder(
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

        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(tomInkluderingsmulighet)

        val adMedEndretInkluderingsmulighet = enAd(
            stillingsId = tomInkluderingsmulighet.stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(adMedEndretInkluderingsmulighet.uuid.toString())

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)
        assertThat(lagretInkluderingsmuligheter.first().tilretteleggingmuligheter).containsExactly("ARBEIDSTID")
        assertThat(lagretInkluderingsmuligheter.last().tilretteleggingmuligheter).isEmpty()
    }

    @Test
    fun `Vi lagrer inkluderingsmuligheter i batch hvis det finnes data men det er en rad og den er tom`() {

        val stillingsid = UUID.randomUUID().toString()

        val adMedEndretInkluderingsmulighet1 = enAd(
            stillingsId = stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]"""
        )

        val adMedEndretInkluderingsmulighet2 = enAd(
            stillingsId = stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__FYSISK"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet1)
        sendMelding(adMedEndretInkluderingsmulighet2)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(stillingsid)

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)
        assertThat(lagretInkluderingsmuligheter[0].tilretteleggingmuligheter).containsExactly("FYSISK")
        assertThat(lagretInkluderingsmuligheter[1].tilretteleggingmuligheter).containsExactly("ARBEIDSTID")
    }

    @Test
    fun `Vi lagrer inkluderingsmuligheter hvis det finnes data men det er en rad og den er tom, og en eldre rad som ikke er tom`() {
        val tomInkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = UUID.randomUUID().toString(),
            radOpprettet = LocalDateTime.now()
        )

        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(Inkluderingsmulighet(
            stillingsid = tomInkluderingsmulighet.stillingsid,
            statligInkluderingsdugnad = true,
            prioriterteMålgrupper = listOf("KOMMER_FRA_LAND_UTENFOR_EØS"),
            radOpprettet = LocalDateTime.now()
        ))
        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(tomInkluderingsmulighet)


        val adMedEndretInkluderingsmulighet = enAd(
            stillingsId = tomInkluderingsmulighet.stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(adMedEndretInkluderingsmulighet.uuid.toString())

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(3)

        assertThat(lagretInkluderingsmuligheter[2].prioriterteMålgrupper).containsExactly("KOMMER_FRA_LAND_UTENFOR_EØS")
        assertThat(lagretInkluderingsmuligheter[2].tilretteleggingmuligheter).isEmpty()
        assertThat(lagretInkluderingsmuligheter[1].harInkludering()).isFalse
        assertThat(lagretInkluderingsmuligheter[0].tilretteleggingmuligheter).containsExactly("ARBEIDSTID")
    }

    @Test
    fun `Vi lagrer inkluderingsmuligheter hvis det finnes data som er annerledes`() {
        val inkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = UUID.randomUUID().toString(),
            statligInkluderingsdugnad = true,
            prioriterteMålgrupper = listOf("KOMMER_FRA_LAND_UTENFOR_EØS"),
            radOpprettet = LocalDateTime.now()
        )

        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(inkluderingsmulighet)


        val adMedEndretInkluderingsmulighet = enAd(
            stillingsId = inkluderingsmulighet.stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(adMedEndretInkluderingsmulighet.uuid.toString())

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)

        assertThat(lagretInkluderingsmuligheter[1].prioriterteMålgrupper).containsExactly("KOMMER_FRA_LAND_UTENFOR_EØS")
        assertThat(lagretInkluderingsmuligheter[1].tilretteleggingmuligheter).isEmpty()
        assertThat(lagretInkluderingsmuligheter[0].tilretteleggingmuligheter).containsExactly("ARBEIDSTID")
    }

    @Test
    fun `Vi lagrer IKKE inkluderingsmuligheter hvis melding er tom og db er tom`() {

        val adMedEndretInkluderingsmulighet = enAdUtenTag()

        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(adMedEndretInkluderingsmulighet.uuid.toString())

        assertThat(lagretInkluderingsmuligheter).isEmpty()
    }

    @Test
    fun `Vi lagrer IKKE inkluderingsmuligheter hvis siste rad og ny melding er tom`() {
        val stillingdId = UUID.randomUUID().toString()
        val inkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = stillingdId,
            statligInkluderingsdugnad = true,
            prioriterteMålgrupper = listOf("KOMMER_FRA_LAND_UTENFOR_EØS"),
            radOpprettet = LocalDateTime.now()
        )
        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(inkluderingsmulighet)

        val tomInkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = stillingdId,
            radOpprettet = LocalDateTime.now()
        )
        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(tomInkluderingsmulighet)

        val adMedEndretInkluderingsmulighet = enAdUtenTag(stillingdId)
        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(stillingdId)

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(2)
        assertThat(lagretInkluderingsmuligheter[1].prioriterteMålgrupper).containsExactly("KOMMER_FRA_LAND_UTENFOR_EØS")
        assertThat(lagretInkluderingsmuligheter[0].harInkludering()).isFalse
    }

    @Test
    fun `Vi lagrer IKKE inkluderingsmuligheter hvis begge er like`() {
        val stillingsid = UUID.randomUUID().toString()
        val inkluderingsmulighet = Inkluderingsmulighet(
            stillingsid = stillingsid,
            tilretteleggingmuligheter = listOf("ARBEIDSTID"),
            radOpprettet = LocalDateTime.now()
        )

        inkluderingsmuligheterRepository.lagreInkluderingsmuligheter(inkluderingsmulighet)

        val adMedEndretInkluderingsmulighet = enAd(
            stillingsId = stillingsid,
            tags = """["INKLUDERING", "INKLUDERING__ARBEIDSTID"]"""
        )

        sendMelding(adMedEndretInkluderingsmulighet)

        ventLitt()

        val lagretInkluderingsmuligheter = inkluderingsmuligheterRepository.hentInkluderingsmulighet(stillingsid)

        assertThat(lagretInkluderingsmuligheter.size).isEqualTo(1)
        assertThat(lagretInkluderingsmuligheter[0].tilretteleggingmuligheter).containsExactly("ARBEIDSTID")
    }

    private fun sendMelding(ad: Ad) {
        mockConsumer.addRecord(ConsumerRecord(stillingstopic, 0, offset++, ad.uuid.toString(), ad))
    }

    private fun ventLitt() = Thread.sleep(300)

*/
}