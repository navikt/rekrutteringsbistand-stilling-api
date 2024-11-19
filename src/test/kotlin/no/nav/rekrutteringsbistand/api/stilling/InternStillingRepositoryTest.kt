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
class InternStillingRepositoryTest {


    @Autowired
    lateinit var repository: InternStillingRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @AfterEach
    fun cleanUp() {
        testRepository.slettAlt()
    }

    @Test
    fun `Skal kunne opprette 2 ulike annonser med forskjellig stillingsid`() {
        val stilling = enStilling
        val stilling2 = enStilling.copy(id = 2000, uuid = UUID.randomUUID().toString(), title = "Stlling 2", reference = UUID.randomUUID().toString())

        val internStilling1 = InternStilling(
            UUID.fromString(stilling.uuid),
            stilling,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        val internStilling2 = InternStilling(
            UUID.fromString(stilling2.uuid),
            stilling2,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling2.createdBy,
            sistEndretAv = stilling2.createdBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        repository.lagreInternStilling(internStilling1)
        repository.lagreInternStilling(internStilling2)

        val hentetStilling1 = repository.getInternStilling(internStilling1.stillingsid.toString())
        val hentetStilling2 = repository.getInternStilling(internStilling2.stillingsid.toString())

        assertNotNull(hentetStilling1)
        assertNotNull(hentetStilling2)

        assertEquals(hentetStilling1.stillingsid, internStilling1.stillingsid)
        assertEquals(hentetStilling2.stillingsid, internStilling2.stillingsid)

        assertEquals("testnss", hentetStilling1.innhold.title)
        assertEquals("Stlling 2", hentetStilling2.innhold.title)
    }
}
