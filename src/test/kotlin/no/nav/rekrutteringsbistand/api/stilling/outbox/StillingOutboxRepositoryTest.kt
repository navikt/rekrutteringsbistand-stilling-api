package no.nav.rekrutteringsbistand.api.stilling.outbox

import no.nav.rekrutteringsbistand.api.TestRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StillingOutboxRepositoryTest {

    @Autowired
    lateinit var stillingOutboxRepository: StillingOutboxRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @AfterEach
    fun cleanUp() {
        testRepository.slettAlt()
    }

    @Test
    fun `Skal sette inn melding som kan leses ut på rikig format`() {
        val uuid = UUID.randomUUID()
        stillingOutboxRepository.lagreMeldingIOutbox(uuid, EventName.INDEKSER_DIREKTEMELDT_STILLING)

        val uprosesserteStillinger = stillingOutboxRepository.finnUprossesertMeldinger()

        assertEquals(1, uprosesserteStillinger.size)

        val stillingOutboxMelding = uprosesserteStillinger.first()

        assertEquals(EventName.INDEKSER_DIREKTEMELDT_STILLING, stillingOutboxMelding.eventName)
        assertEquals(uuid, stillingOutboxMelding.stillingsId)
    }
}
