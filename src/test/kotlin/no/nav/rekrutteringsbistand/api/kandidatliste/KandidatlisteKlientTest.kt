package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("local", "kandidatlisteMock")
class KandidatlisteKlientTest {

    @Autowired
    lateinit var klient: KandidatlisteKlient

    @Test
    fun `Skal kunne oppdatere en kandidatliste`() {
        val response = klient.oppdaterKandidatliste(Stillingsid(UUID.randomUUID()))
        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
    }
}
