package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirektemeldtStillingRepositoryTest {


    @Autowired
    lateinit var repository: DirektemeldtStillingRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @AfterEach
    fun cleanUp() {
        testRepository.slettAlt()
    }

    @Test
    fun `Skal kunne opprette 2 ulike annonser med forskjellig stillingsid`() {
        val stilling = enStilling
        val stilling2 = enStilling.copy(id = 2000, uuid = UUID.randomUUID().toString(), title = "Stilling 2", reference = UUID.randomUUID().toString())

        val direktemeldtStilling1 = DirektemeldtStilling(
            UUID.fromString(stilling.uuid),
            stilling.toDirektemeldtStillingBlob(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling.status
        )
        val direktemeldtStilling2 = DirektemeldtStilling(
            UUID.fromString(stilling2.uuid),
            stilling2.toDirektemeldtStillingBlob(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling2.createdBy,
            sistEndretAv = stilling2.createdBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling2.status,
        )
        repository.lagreDirektemeldtStilling(direktemeldtStilling1)
        repository.lagreDirektemeldtStilling(direktemeldtStilling2)

        val hentetStilling1 = repository.hentDirektemeldtStilling(direktemeldtStilling1.stillingsid.toString())
        val hentetStilling2 = repository.hentDirektemeldtStilling(direktemeldtStilling2.stillingsid.toString())

        assertNotNull(hentetStilling1)
        assertNotNull(hentetStilling2)

        assertEquals(hentetStilling1.stillingsid, direktemeldtStilling1.stillingsid)
        assertEquals(hentetStilling2.stillingsid, direktemeldtStilling2.stillingsid)

        assertEquals("testnss", hentetStilling1.innhold.title)
        assertEquals("Stilling 2", hentetStilling2.innhold.title)
    }
}
