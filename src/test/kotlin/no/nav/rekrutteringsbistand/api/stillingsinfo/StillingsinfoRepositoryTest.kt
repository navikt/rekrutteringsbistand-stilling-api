package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoOppdatering
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

    @Autowired
    lateinit var testRepository: TestRepository

    @Test
    fun `Skal kunne lagre og hente ut stillingsinfo`() {
        repository.opprett(tilLagring)
        val lagretStillingsinfo: Option<Stillingsinfo> = repository.hentForStilling(tilLagring.stillingsid)

        assertThat(lagretStillingsinfo).isEqualTo(tilLagring.toOption())
    }

    @Test
    fun `Skal kunne lagre og hente ut flere stillingsinfoer om gangen`() {
        val stillingsinfo1 = enStillingsinfo
        val stillingsinfo2 = enAnnenStillingsinfo

        repository.opprett(stillingsinfo1)
        repository.opprett(stillingsinfo2)

        val lagretStillingsinfoer = repository.hentForStillinger(
            listOf(stillingsinfo1.stillingsid, stillingsinfo2.stillingsid)
        )

        assertThat(lagretStillingsinfoer.first()).isEqualTo(stillingsinfo1)
        assertThat(lagretStillingsinfoer.last()).isEqualTo(stillingsinfo2)
    }

    @Test
    fun `Skal kunne oppdatere eierident og eiernavn på stillingsinfo`() {
        repository.opprett(tilLagring)
        repository.oppdaterEier(enStillingsinfoOppdatering.stillingsinfoid, enStillingsinfoOppdatering.eier)

        val endretRekrutteringsbistand =
            repository.hentForStilling(tilLagring.stillingsid).getOrElse { fail("Testsetup") }

        assertThat(endretRekrutteringsbistand.eier?.navident).isEqualTo(enStillingsinfoOppdatering.eier.navident)
        assertThat(endretRekrutteringsbistand.eier?.navn).isEqualTo(enStillingsinfoOppdatering.eier.navn)
    }

    @After
    fun cleanUp() {
        testRepository.slettAlt()
    }
}
