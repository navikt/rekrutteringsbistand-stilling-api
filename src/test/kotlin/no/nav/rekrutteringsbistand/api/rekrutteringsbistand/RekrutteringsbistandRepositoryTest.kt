package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import arrow.core.Some
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandOppdatering
import no.nav.rekrutteringsbistand.api.Testdata.etRekrutteringsbistand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest
@ActiveProfiles("local")
class RekrutteringsbistandRepositoryTest {

    val tilLagring = etRekrutteringsbistand

    @Autowired
    lateinit var repository: RekrutteringsbistandRepository

    @Test
    fun `skal kunne lagre og hente ut rekrutteringsbistand`() {
        repository.lagre(tilLagring)
        val lagretRekrutteringsbistand = repository.hentForStilling(tilLagring.stillingId)

        assertThat(lagretRekrutteringsbistand).isEqualTo(Some(tilLagring))
    }

    @Test
    fun `Skal kunne oppdatere eierident og eiernavn på rekrutteringsbistand`() {
        repository.lagre(tilLagring)
        repository.oppdaterEierIdentOgEierNavn(enRekrutteringsbistandOppdatering)

        val endretRekrutteringsbistand = repository.hentForStilling(tilLagring.stillingId).getOrElse { fail("Testsetup") }

        assertThat(endretRekrutteringsbistand.eier.ident).isEqualTo(enRekrutteringsbistandOppdatering.eierIdent)
        assertThat(endretRekrutteringsbistand.eier.navn).isEqualTo(enRekrutteringsbistandOppdatering.eierNavn)
    }

    @After
    fun cleanUp() {
        repository.slett(tilLagring.rekrutteringId)
    }
}
