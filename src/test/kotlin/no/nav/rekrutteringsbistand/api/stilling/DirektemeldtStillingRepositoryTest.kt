package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enDirektemeldtStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.stillingSomIkkeSkalAktiveres
import no.nav.rekrutteringsbistand.api.Testdata.stillingSomIkkeSkalDeaktiveres
import no.nav.rekrutteringsbistand.api.Testdata.stillingerSomSkalAktiveres
import no.nav.rekrutteringsbistand.api.Testdata.stillingerSomSkalDeaktiveres
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
            stilling.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling.status,
            annonseId = 1
        )
        val direktemeldtStilling2 = DirektemeldtStilling(
            UUID.fromString(stilling2.uuid),
            stilling2.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling2.createdBy,
            sistEndretAv = stilling2.createdBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling2.status,
            annonseId = 2
        )
        repository.lagreDirektemeldtStilling(direktemeldtStilling1)
        repository.lagreDirektemeldtStilling(direktemeldtStilling2)

        val hentetStilling1 = repository.hentDirektemeldtStilling(direktemeldtStilling1.stillingsId.toString())
        val hentetStilling2 = repository.hentDirektemeldtStilling(direktemeldtStilling2.stillingsId.toString())

        assertNotNull(hentetStilling1)
        assertNotNull(hentetStilling2)

        assertEquals(hentetStilling1?.stillingsId, direktemeldtStilling1.stillingsId)
        assertEquals(hentetStilling2?.stillingsId, direktemeldtStilling2.stillingsId)

        assertEquals("testnss", hentetStilling1?.innhold?.title)
        assertEquals("Stilling 2", hentetStilling2?.innhold?.title)
    }

    @Test
    fun `Skal finne direktemeldte stillinger som skal deaktiveres`() {
        val kandidaterForDeaktivering = stillingerSomSkalDeaktiveres

        kandidaterForDeaktivering.forEach {
            repository.lagreDirektemeldtStilling(it)
        }
        repository.lagreDirektemeldtStilling(stillingSomIkkeSkalDeaktiveres)

        val deaktiveringskandidater = repository.hentStillingerForDeaktivering()

        assertEquals(3, deaktiveringskandidater.size)

        assertEquals("Stilling 1", deaktiveringskandidater[0].innhold.title)
        assertEquals("Stilling 2", deaktiveringskandidater[1].innhold.title)
        assertEquals("Stilling 3", deaktiveringskandidater[2].innhold.title)
    }

    @Test
    fun `Skal finne direktemeldte stillinger som skal bli aktivert`() {
        val stillingerForAktivering = stillingerSomSkalAktiveres

        stillingerForAktivering.forEach {
            repository.lagreDirektemeldtStilling(it)
        }
        repository.lagreDirektemeldtStilling(stillingSomIkkeSkalAktiveres)

        val stillinger = repository.hentStillingerForAktivering()

        assertEquals(2, stillinger.size)

        assertEquals("Stilling 4", stillinger[0].innhold.title)
        assertEquals("Stilling 12", stillinger[1].innhold.title)
    }

    @Test
    fun `Skal ikke finne noen stillinger hvis det ikke er noen som skal deaktiveres`() {
        val stilling = enDirektemeldtStilling.copy(innhold = enDirektemeldtStilling.innhold.copy(expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10)))

        repository.lagreDirektemeldtStilling(stilling)

        val stillingerForDeaktivering = repository.hentStillingerForDeaktivering()

        assertEquals(0, stillingerForDeaktivering.size)
    }

    @Test
    fun `Skal ikke finne noen stillinger hvis det ikke er noen som skal aktiveres`() {
        val stilling = enDirektemeldtStilling.copy(innhold = enDirektemeldtStilling.innhold.copy(expires = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10)))

        repository.lagreDirektemeldtStilling(stilling)

        val stillingerForAktivering = repository.hentStillingerForAktivering()

        assertEquals(0, stillingerForAktivering.size)
    }

    @Test
    fun `Skal returnere tom liste hvis det ikke er noen direktemeldte stillinger i tabellen`() {
        val direktemeldteStillinger = repository.hentAlleDirektemeldteStillinger()

        assertEquals(0, direktemeldteStillinger.size)
    }
}
