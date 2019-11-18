package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandOppdatering
import no.nav.rekrutteringsbistand.api.Testdata.etRekrutteringsbistand
import org.assertj.core.api.Assertions.assertThat
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
        val lagretRekrutteringsbistand = repository.hentForStilling(tilLagring.stillingUuid)

        assertThat(lagretRekrutteringsbistand).isEqualTo(tilLagring)
    }

    @Test
    fun `skal kunne oppdatere eier ident og eier navn på rekrutteringsbistand`() {
        repository.lagre(tilLagring)
        repository.oppdaterEierIdentOgEierNavn(enRekrutteringsbistandOppdatering)

        val endretRekrutteringsbistand = repository.hentForStilling(tilLagring.stillingUuid)

        assertThat(endretRekrutteringsbistand.eierIdent).isEqualTo(enRekrutteringsbistandOppdatering.eierIdent)
        assertThat(endretRekrutteringsbistand.eierNavn).isEqualTo(enRekrutteringsbistandOppdatering.eierNavn)
    }

    @After
    fun cleanUp() {
        repository.slett(tilLagring.rekrutteringUuid!!)
    }
}
