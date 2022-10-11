package no.nav.rekrutteringsbistand.api.hendelser

import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.isMissingOrNull
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.rekrutteringsbistand.api.hendelser.RapidApplikasjon.Companion.registrerLyttere
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingsinfopopulatorTest {


    @Autowired private lateinit var stillingsinfoRepository: StillingsinfoRepository
    @Autowired private lateinit var context: ApplicationContext
    private lateinit var testRapid: TestRapid


    @Before fun setUp(){
        if(!this::testRapid.isInitialized) testRapid = TestRapid().registrerLyttere(stillingsinfoRepository, context)
        testRapid.reset()
    }
    @Test
    fun `populering av en stilling`() {
        val stillingsinfoid = Stillingsinfoid(UUID.randomUUID())
        val stillingsId = Stillingsid(UUID.randomUUID())
        val eier = Eier("AB123456", "Navnesen")
        val notat = "Et notat"
        val stillingskategori = Stillingskategori.ARBEIDSTRENING
        val stillingsinfo = Stillingsinfo(stillingsinfoid, stillingsId, eier, notat, stillingskategori)
        stillingsinfoRepository.opprett(stillingsinfo)
        testRapid.sendTestMessage("""
            {
                "uinteressant": "felt",
                "kandidathendelse": {
                    "uinteressant2": "felt2",
                    "stillingsId": "${stillingsId.asString()}"
                }
            }
        """.trimIndent())
        assertEquals(1, testRapid.inspektør.size)
        val message = testRapid.inspektør.message(0)
        assertEquals("felt", message.get("uinteressant").asText())
        assertEquals("felt2", message.path("kandidathendelse").get("uinteressant2").asText())
        assertEquals(stillingsId.asString(), message.path("kandidathendelse").get("stillingsId").asText())
        val stillingNode = message.path("stilling")
        assertFalse(stillingNode.isMissingOrNull())
        assertEquals(stillingsinfo.stillingsinfoid.asString(), stillingNode.path("stillingsinfoid").asText())
        assertEquals(stillingsinfo.stillingsid.asString(), stillingNode.path("stillingsid").asText())
        assertEquals(stillingsinfo.stillingskategori?.name, stillingNode.path("stillingskategori").asText())
        assertEquals(stillingsinfo.eier!!.navident, stillingNode.path("eier").path("navident").asText())
        assertEquals(stillingsinfo.eier!!.navn, stillingNode.path("eier").path("navn").asText())
        assertEquals(stillingsinfo.notat, stillingNode.path("notat").asText())
    }

    @Test fun `hendelse uten stillingsid skal ikke populeres`() {
        testRapid.sendTestMessage("""
            {
                "uinteressant": "felt",
                "kandidathendelse": {
                    "uinteressant2": "felt2",
                }
            }
        """.trimIndent())
        assertEquals(0, testRapid.inspektør.size)
    }

    @Test fun `hendelse med stillingsid og utfyllt stilling skal ignoreres`() {
        testRapid.sendTestMessage("""
            {
                "uinteressant": "felt",
                "kandidathendelse": {
                    "uinteressant2": "felt2",
                },
                "stilling": {}
            }
        """.trimIndent())
        assertEquals(0, testRapid.inspektør.size)
    }
}