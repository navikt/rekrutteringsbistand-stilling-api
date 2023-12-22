package no.nav.rekrutteringsbistand.api.indekser

import no.nav.rekrutteringsbistand.api.skjul_stilling.SkjulStillingRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IndekserComponentTest {
    @Autowired
    lateinit var stillingsinfoRepository: StillingsinfoRepository

    @Autowired
    lateinit var skjulStillingRepository: SkjulStillingRepository

    @Autowired
    lateinit var restTemplate: TestRestTemplate  // TODO: Ser vi initierer direkte uten injection andre steder, blir dette riktig template

    fun `TODO Skal kunne lagre og hente`() {

    }
}