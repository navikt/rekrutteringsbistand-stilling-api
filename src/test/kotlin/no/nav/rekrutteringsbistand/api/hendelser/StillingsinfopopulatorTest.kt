package no.nav.rekrutteringsbistand.api.hendelser

import arrow.core.Some
import arrow.core.getOrElse
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.asZonedDateTime
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon.Companion.registrerLyttere
import no.nav.rekrutteringsbistand.api.stilling.Arbeidsgiver
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingsinfopopulatorTest {


    @Autowired
    private lateinit var stillingsinfoRepository: StillingsinfoRepository

    @Autowired
    private lateinit var context: ApplicationContext

    @MockBean
    private lateinit var arbeidsplassenKlient: ArbeidsplassenKlient
    private lateinit var testRapid: TestRapid


    @Before
    fun setUp() {
        if (!this::testRapid.isInitialized) testRapid =
            TestRapid().registrerLyttere(stillingsinfoRepository, context, arbeidsplassenKlient)
        testRapid.reset()
    }

    @Test
    fun `populering av en stilling`() {
        val stillingsId = Stillingsid(UUID.randomUUID())
        val stillingsTittel = "Klovn på sirkus"
        val eksternStillingskilde = "ASS"
        val stillingstidspunkt = LocalDateTime.now()
        val antallStillinger = 666
        val organisasjonsnummer = "123"
        val stillingensPubliseringstidspunkt = LocalDateTime.now()

        Mockito.`when`(arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.toString()))
            .thenReturn(
                Some(
                    enStilling.copy(
                        title = stillingsTittel,
                        source = eksternStillingskilde,
                        publishedByAdmin = stillingstidspunkt.toString(),
                        properties = mapOf("positioncount" to "$antallStillinger"),
                        employer = Arbeidsgiver(
                            null, null, null, null, null,
                            null, emptyList(), emptyList(), null, emptyList(),
                            emptyMap(), null,
                            organisasjonsnummer,
                            null, null, null, null, null, null
                        ),
                        published = stillingensPubliseringstidspunkt
                    )
                )
            )
        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val notat = "Et notat"
        val stillingskategori = Stillingskategori.ARBEIDSTRENING
        val stillingsinfo = Stillingsinfo(stillingsinfoid, stillingsId, eier, notat, stillingskategori)
        stillingsinfoRepository.opprett(stillingsinfo)
        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
                "stillingsId": "${stillingsId.asString()}"
            }
        """.trimIndent()
        )
        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("felt", message.get("uinteressant").asText())
        assertEquals("felt2", message.path("uinteressant2").asText())
        assertEquals(stillingsId.asString(), message.path("stillingsId").asText())
        assertEquals(stillingsTittel, message.path("stilling").get("stillingstittel").asText())
        assertEquals(stillingsTittel=="DIR", message.path("stilling").get("erDirektemeldt").asBoolean())
        assertEquals(
            ZonedDateTime.of(stillingstidspunkt, ZoneId.of("Europe/Oslo")),
            message.path("stilling").get("stillingOpprettetTidspunkt").asZonedDateTime()
        )
        assertEquals(antallStillinger, message.path("stilling").get("antallStillinger").asInt())
        assertEquals(organisasjonsnummer, message.path("stilling").get("organisasjonsnummer").asText())
        assertEquals(
            ZonedDateTime.of(stillingensPubliseringstidspunkt, ZoneId.of("Europe/Oslo")),
            ZonedDateTime.parse(message.path("stilling").get("stillingensPubliseringstidspunkt").asText()).toInstant().atZone(ZoneId.of("Europe/Oslo"))
        )
        assertFalse(message.path("stilling").get("erDirektemeldt").asBoolean())
        val stillingNode = message.path("stillingsinfo")
        assertFalse(stillingNode.isMissingOrNull())
        assertEquals(stillingsinfo.stillingsinfoid.asString(), stillingNode.path("stillingsinfoid").asText())
        assertEquals(stillingsinfo.stillingsid.asString(), stillingNode.path("stillingsid").asText())
        assertEquals(stillingsinfo.stillingskategori?.name, stillingNode.path("stillingskategori").asText())
        assertEquals(stillingsinfo.eier!!.navident, stillingNode.path("eier").path("navident").asText())
        assertEquals(stillingsinfo.eier!!.navn, stillingNode.path("eier").path("navn").asText())
        assertEquals(stillingsinfo.notat, stillingNode.path("notat").asText())
    }

    @Test
    fun `skal lagre stillinginfo og publisere ny melding når vi mottar hendelse for stilling uten stillingsinfo`() {
        val stillingsId = Stillingsid(UUID.randomUUID())
        Mockito.`when`(arbeidsplassenKlient.hentStillingBasertPåUUID(stillingsId.toString()))
            .thenReturn(Some(enStilling.copy(title = "Dummy-tittel")))
        stillingsinfoRepository.hentForStilling(stillingsId).tap {
            fail("Setup")
        }

        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
                "stillingsId": "${stillingsId.asString()}"
            }
        """.trimIndent()
        )

        val lagretStillingsinfo = stillingsinfoRepository.hentForStilling(stillingsId).getOrElse {
            fail("Stillingsinfo ikke lagret")
        }
        assertNull(lagretStillingsinfo.eier)
        assertNull(lagretStillingsinfo.notat)
        assertNull(lagretStillingsinfo.stillingskategori)
        assertThat(lagretStillingsinfo.stillingsid).isEqualTo(stillingsId)
        assertNotNull(lagretStillingsinfo.stillingsinfoid)

        assertThat(testRapid.inspektør.size).isOne
        val message = testRapid.inspektør.message(0)
        val stillingsinfo = message.path("stillingsinfo")
        assertThat(stillingsinfo.path("stillingsinfoid").asText()).isEqualTo(lagretStillingsinfo.stillingsinfoid.toString())
        assertThat(stillingsinfo.path("stillingsid").asText()).isEqualTo(lagretStillingsinfo.stillingsid.toString())
        assertTrue(stillingsinfo.path("stillingskategori").isNull)
        assertTrue(stillingsinfo.path("eier").isNull)
        assertTrue(stillingsinfo.path("stillingskategori").isNull)
    }

    @Test
    fun `hendelse uten stillingsid skal ikke populeres`() {
        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
            }
        """.trimIndent()
        )
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `hendelse med stillingsid og utfyllt stillingsinfo skal ignoreres`() {
        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
                "stillingsId": "123",
                "stillingsinfo": {}
            }
        """.trimIndent()
        )
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test
    fun `hendelse med stillingsid og utfyllt stilling skal ignoreres`() {
        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
                "stillingsId": "123",
                "stilling": {}
            }
        """.trimIndent()
        )
        assertEquals(0, testRapid.inspektør.size)
    }
}
