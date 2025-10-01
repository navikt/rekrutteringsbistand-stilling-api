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
import org.junit.jupiter.api.Assertions.assertTrue
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
        val stilling2 = enStilling.copy(uuid = UUID.randomUUID().toString(), title = "Stilling 2", reference = UUID.randomUUID().toString())

        val direktemeldtStilling1 = DirektemeldtStilling(
            UUID.fromString(stilling.uuid),
            stilling.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling.status,
            annonsenr = "1",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(3),
            publisert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            publisertAvAdmin = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toString(),
            adminStatus = "DONE"
        )
        val direktemeldtStilling2 = DirektemeldtStilling(
            UUID.fromString(stilling2.uuid),
            stilling2.toDirektemeldtStillingInnhold(),
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling2.createdBy,
            sistEndretAv = stilling2.createdBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = stilling2.status,
            annonsenr = "2",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(3),
            publisert = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            publisertAvAdmin = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toString(),
            adminStatus = "DONE"
        )
        repository.lagreDirektemeldtStilling(direktemeldtStilling1)
        repository.lagreDirektemeldtStilling(direktemeldtStilling2)

        val hentetStilling1 = repository.hentDirektemeldtStilling(direktemeldtStilling1.stillingsId.toString())
        val hentetStilling2 = repository.hentDirektemeldtStilling(direktemeldtStilling2.stillingsId.toString())

        assertNotNull(hentetStilling1)
        assertNotNull(hentetStilling2)

        assertEquals(hentetStilling1?.stillingsId, direktemeldtStilling1.stillingsId)
        assertEquals(hentetStilling2?.stillingsId, direktemeldtStilling2.stillingsId)

        assertEquals("Stilling uten valgt jobbtittel", hentetStilling1?.innhold?.title)
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
        val stilling = enDirektemeldtStilling.copy(utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10))

        repository.lagreDirektemeldtStilling(stilling)

        val stillingerForDeaktivering = repository.hentStillingerForDeaktivering()

        assertEquals(0, stillingerForDeaktivering.size)
    }

    @Test
    fun `Skal ikke finne noen stillinger hvis det ikke er noen som skal aktiveres`() {
        val stilling = enDirektemeldtStilling.copy(utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).plusDays(10))

        repository.lagreDirektemeldtStilling(stilling)

        val stillingerForAktivering = repository.hentStillingerForAktivering()

        assertEquals(0, stillingerForAktivering.size)
    }

    @Test
    fun `Skal returnere tom liste hvis det ikke er noen direktemeldte stillinger i tabellen`() {
        val direktemeldteStillinger = repository.hentAlleStillingsIder()

        assertEquals(0, direktemeldteStillinger.size)
    }

    @Test
    fun `Skal returnere en liste med UUID-er hvis det er direktemeldte stillinger i tabellen`() {
        val stilling = enDirektemeldtStilling.copy(stillingsId = UUID.randomUUID())
        repository.lagreDirektemeldtStilling(stilling)

        val direktemeldteStillinger = repository.hentAlleStillingsIder()

        assertEquals(1, direktemeldteStillinger.size)
        assertEquals(stilling.stillingsId, direktemeldteStillinger[0])
    }

    @Test
    fun `hentUtgåtteStillingerFor6mndSidenSomErPending skal finne en stilling med utløpsdato over 6mnd`() {
        val stilling = enDirektemeldtStilling.copy(
            adminStatus = "PENDING",
            utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(6).minusDays(1)
        )
        repository.lagreDirektemeldtStilling(stilling)

        val søkeresultat = repository.hentUtgåtteStillingerFor6mndSidenSomErPending()
        assertEquals(1, søkeresultat.size)
    }

    @Test
    fun `hentUtgåtteStillingerFor6mndSidenSomErPending skal ikke finne stillinger med utløpsdato nyere en 6mnd eller andre statuser en DONE`() {
        val stillingerSomIkkeSkalGiTreff = listOf(
            enDirektemeldtStilling.copy(
                adminStatus = "PENDING",
                utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(6).plusDays(1)
            ),
            enDirektemeldtStilling.copy(
                adminStatus = "PENDING",
                utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
            ),
            enDirektemeldtStilling.copy(
                adminStatus = "DONE",
                utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(6)
            ),
            enDirektemeldtStilling.copy(
                adminStatus = "REJECTED",
                utløpsdato = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).minusMonths(6)
            ),
        )
        stillingerSomIkkeSkalGiTreff.forEach {
            repository.lagreDirektemeldtStilling(it)
        }

        assertTrue(repository.hentUtgåtteStillingerFor6mndSidenSomErPending().isEmpty())
    }

    @Test
    fun `settAnnonsenrFraDbId skal oppdatere annonsenr for en direktemeldt stilling`() {
        val stilling = enDirektemeldtStilling
        repository.lagreDirektemeldtStilling(stilling)

        repository.settAnnonsenrFraDbId(stilling.stillingsId.toString())

        val oppdatertStilling = repository.hentDirektemeldtStilling(stilling.stillingsId.toString())
        assertNotNull(oppdatertStilling)
        assertTrue(oppdatertStilling?.annonsenr?.startsWith("R") ?: false)
    }
}
