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
class FrontendStillingOutboxRepositoryTest {

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

        val uprosesserteStillinger = stillingOutboxRepository.finnBatchMedUprossesertMeldinger()

        assertEquals(1, uprosesserteStillinger.size)

        val stillingOutboxMelding = uprosesserteStillinger.first()

        assertEquals(EventName.INDEKSER_DIREKTEMELDT_STILLING, stillingOutboxMelding.eventName)
        assertEquals(uuid, stillingOutboxMelding.stillingsId)
    }

    @Test
    fun `Skal hente indekser-eventer før reindekser-eventer`() {
        stillingOutboxRepository.lagreMeldingIOutbox(UUID.randomUUID(), EventName.REINDEKSER_DIREKTEMELDT_STILLING)
        stillingOutboxRepository.lagreMeldingIOutbox(UUID.randomUUID(), EventName.REINDEKSER_DIREKTEMELDT_STILLING)
        stillingOutboxRepository.lagreMeldingIOutbox(UUID.randomUUID(), EventName.REINDEKSER_DIREKTEMELDT_STILLING)
        stillingOutboxRepository.lagreMeldingIOutbox(UUID.randomUUID(), EventName.REINDEKSER_DIREKTEMELDT_STILLING)
        stillingOutboxRepository.lagreMeldingIOutbox(UUID.randomUUID(), EventName.INDEKSER_DIREKTEMELDT_STILLING)

        val uprosesserteStillinger = stillingOutboxRepository.finnBatchMedUprossesertMeldinger()

        assertEquals(5, uprosesserteStillinger.size)
        assertEquals(uprosesserteStillinger.first().eventName, EventName.INDEKSER_DIREKTEMELDT_STILLING)
    }
}
