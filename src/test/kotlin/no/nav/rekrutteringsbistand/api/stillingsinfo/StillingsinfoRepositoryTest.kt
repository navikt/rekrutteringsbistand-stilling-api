package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoOppdatering
import no.nav.rekrutteringsbistand.api.option.Option
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StillingsinfoRepositoryTest {

    val tilLagring = enStillingsinfo

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Test
    fun `Skal kunne lagre og hente ut stillingsinfo`() {
        repository.lagre(tilLagring)
        val lagretRekrutteringsbistand: Option<Stillingsinfo> = repository.hentForStilling(tilLagring.stillingsid)

        assertThat(lagretRekrutteringsbistand).isEqualTo(Option(tilLagring))
    }

    @Test
    fun `Skal kunne oppdatere eierident og eiernavn på stillingsinfo`() {
        repository.lagre(tilLagring)
        repository.oppdaterEierIdentOgEierNavn(enStillingsinfoOppdatering)

        val endretRekrutteringsbistand =
            repository.hentForStilling(tilLagring.stillingsid).getOrElse { fail("Testsetup") }

        assertThat(endretRekrutteringsbistand.eier?.navident).isEqualTo(enStillingsinfoOppdatering.eier.navident)
        assertThat(endretRekrutteringsbistand.eier?.navn).isEqualTo(enStillingsinfoOppdatering.eier.navn)
    }

    @After
    fun cleanUp() {
        repository.slett(tilLagring.stillingsinfoid)
    }
}
