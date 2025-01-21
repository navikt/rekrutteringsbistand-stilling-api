package no.nav.rekrutteringsbistand.api.hendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.asZonedDateTime
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon.Companion.registrerLyttere
import no.nav.rekrutteringsbistand.api.stilling.Arbeidsgiver
import no.nav.rekrutteringsbistand.api.stilling.Kategori
import no.nav.rekrutteringsbistand.api.stilling.StillingService
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
import java.time.temporal.ChronoUnit
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingPopulatorTest {


    @Autowired
    private lateinit var context: ApplicationContext

    @MockBean
    private lateinit var stillingService: StillingService
    private lateinit var testRapid: TestRapid


    @Before
    fun setUp() {
        if (!this::testRapid.isInitialized) testRapid =
            TestRapid().registrerLyttere(context, stillingService)
        testRapid.reset()
    }

    @Test
    fun `populering av en stilling`() {
        val stillingsId = Stillingsid(UUID.randomUUID())
        val stillingsTittel = "Klovn på sirkus"
        val eksternStillingskilde = "ASS"
        val stillingstidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        val antallStillinger = 666
        val organisasjonsnummer = "123"
        val stillingensPubliseringstidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)

        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val stillingskategori = Stillingskategori.STILLING
        val stillingsinfoDto = StillingsinfoDto(stillingsId.asString(), stillingsinfoid.asString(), eier.navident, eier.navn, stillingskategori)

        Mockito.`when`(stillingService.hentRekrutteringsbistandStilling(stillingsId.toString(), somSystembruker = true))
            .thenReturn(
                RekrutteringsbistandStilling(
                    stillingsinfoDto,
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
        assertEquals(stillingsTittel == "DIR", message.path("stilling").get("erDirektemeldt").asBoolean())
        assertEquals(
            ZonedDateTime.of(stillingstidspunkt, ZoneId.of("Europe/Oslo")),
            message.path("stilling").get("stillingOpprettetTidspunkt").asZonedDateTime().toInstant()
                .atZone(ZoneId.of("Europe/Oslo"))
        )
        assertEquals(antallStillinger, message.path("stilling").get("antallStillinger").asInt())
        assertEquals(organisasjonsnummer, message.path("stilling").get("organisasjonsnummer").asText())
        assertEquals(
            ZonedDateTime.of(stillingensPubliseringstidspunkt, ZoneId.of("Europe/Oslo")),
            ZonedDateTime.parse(message.path("stilling").get("stillingensPubliseringstidspunkt").asText()).toInstant()
                .atZone(ZoneId.of("Europe/Oslo"))
        )
        assertFalse(message.path("stilling").get("erDirektemeldt").asBoolean())
        val stillingNode = message.path("stillingsinfo")
        assertFalse(stillingNode.isMissingOrNull())
        assertEquals(stillingsinfoDto.stillingsinfoid, stillingNode.path("stillingsinfoid").asText())
        assertEquals(stillingsinfoDto.stillingsid, stillingNode.path("stillingsid").asText())
        assertEquals(stillingsinfoDto.stillingskategori?.name, stillingNode.path("stillingskategori").asText())
        assertEquals(stillingsinfoDto.eierNavident, stillingNode.path("eier").path("navident").asText())
        assertEquals(stillingsinfoDto.eierNavn, stillingNode.path("eier").path("navn").asText())
    }

    fun enStillingMed(
        tittel: String,
        source: String,
        categoryList: List<Kategori>
    ) =
        enStilling.copy(
            title = tittel,
            source = source,
            publishedByAdmin = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS).toString(),
            properties = mapOf("positioncount" to "1"),
            employer = Arbeidsgiver(
                null, null, null, null, null,
                null, emptyList(), emptyList(), null, emptyList(),
                emptyMap(), null,
                "123",
                null, null, null, null, null, null
            ),
            published = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
            categoryList = categoryList,
        )

    @Test
    fun `populering av en direktemeldt stilling bruker janzz i stillingstittel hvis ikke styrk08Nav finnes`() {
        val stillingsId = Stillingsid(UUID.randomUUID())

        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val stillingskategori = Stillingskategori.STILLING
        val stillingsinfoDto = StillingsinfoDto(stillingsId.asString(), stillingsinfoid.asString(), eier.navident, eier.navn, stillingskategori)


        Mockito.`when`(stillingService.hentRekrutteringsbistandStilling(stillingsId.toString(), somSystembruker = true))
            .thenReturn(
                RekrutteringsbistandStilling(
                    stillingsinfoDto,
                    enStillingMed(
                        tittel = "Tittel fra arbeidsplassen",
                        source = "DIR",
                        categoryList = listOf(
                            Kategori(
                                name = "Kokk",
                                code = "0000",
                                id = null,
                                categoryType = null,
                                description = null,
                                parentId = null,
                            ), Kategori(
                                name = "Statsminister",
                                code = "0000.00",
                                id = null,
                                categoryType = "JANZZ",
                                description = null,
                                parentId = null,
                            )
                        )
                    )
                )
            )

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

        assertEquals("Statsminister", message.path("stilling").get("stillingstittel").asText())
    }

    @Test
    fun `populering av en direktemeldt stilling bruker styrk08Nav i stillingstittel hvis janzz og Styrk08 finnes`() {
        val stillingsId = Stillingsid(UUID.randomUUID())

        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val stillingskategori = Stillingskategori.STILLING
        val stillingsinfoDto = StillingsinfoDto(stillingsId.asString(), stillingsinfoid.asString(), eier.navident, eier.navn, stillingskategori)


        Mockito.`when`(stillingService.hentRekrutteringsbistandStilling(stillingsId.toString(), somSystembruker = true))
            .thenReturn(
                RekrutteringsbistandStilling(
                    stillingsinfoDto,
                    enStillingMed(
                        tittel = "Tittel fra arbeidsplassen",
                        source = "DIR",
                        categoryList = listOf(
                            Kategori(
                                name = "Kokk",
                                code = "0000.00",
                                id = null,
                                categoryType = null,
                                description = null,
                                parentId = null,
                            ),
                            Kategori(
                                name = "Statsminister",
                                code = "000000",
                                id = null,
                                categoryType = "JANZZ",
                                description = null,
                                parentId = null,
                            )
                        )
                    )
                )
            )

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

        assertEquals("Kokk", message.path("stilling").get("stillingstittel").asText())
    }

    @Test
    fun `populering av en ekstern stilling bruker tittel fra arbeidsplassen`() {
        val stillingsId = Stillingsid(UUID.randomUUID())

        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val stillingskategori = Stillingskategori.STILLING
        val stillingsinfoDto = StillingsinfoDto(stillingsId.asString(), stillingsinfoid.asString(), eier.navident, eier.navn, stillingskategori)


        Mockito.`when`(stillingService.hentRekrutteringsbistandStilling(stillingsId.toString(), somSystembruker = true))
            .thenReturn(
                RekrutteringsbistandStilling(
                    stillingsinfoDto,
                    enStillingMed(
                        tittel = "Tittel fra arbeidsplassen",
                        source = "AMEDIA",
                        categoryList = listOf(
                            Kategori(
                                name = "Kokk",
                                code = "0000.00",
                                id = null,
                                categoryType = null,
                                description = null,
                                parentId = null,
                            )
                        )
                    )
                )
            )


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

        assertEquals("Tittel fra arbeidsplassen", message.path("stilling").get("stillingstittel").asText())
    }

    @Test
    fun `skal ikke lagre stillinginfo og publisere ny melding når vi mottar hendelse for stilling uten stillingsinfo`() {
        val stillingsId = Stillingsid(UUID.randomUUID())

        val rekrutteringsbistandStilling = RekrutteringsbistandStilling(
            null,
            enStilling.copy(title = "Dummy-tittel")
        )
        Mockito.`when`(stillingService.hentRekrutteringsbistandStilling(stillingsId.toString(), somSystembruker = true))
            .thenReturn(
                rekrutteringsbistandStilling
            )

        testRapid.sendTestMessage(
            """
            {
                "uinteressant": "felt",
                "uinteressant2": "felt2",
                "stillingsId": "${stillingsId.asString()}"
            }
        """.trimIndent()
        )

        assertNull(rekrutteringsbistandStilling.stillingsinfo)

        assertThat(testRapid.inspektør.size).isOne
        val message = testRapid.inspektør.message(0)
        assertTrue(message.path("stillingsinfo").isMissingNode)
        assertFalse(message.path("stilling").isMissingOrNull())
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
