package no.nav.rekrutteringsbistand.api.indekser

import no.nav.rekrutteringsbistand.api.Testdata
import no.nav.rekrutteringsbistand.api.skjul_stilling.SkjulStillingRepository
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoInboundDto
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IndekserComponentTest {
    @Autowired
    lateinit var stillingsinfoRepository: StillingsinfoRepository

    @Autowired
    lateinit var skjulStillingRepository: SkjulStillingRepository

    @Autowired
    lateinit var restTemplate: TestRestTemplate  // TODO: Ser vi initierer direkte uten injection andre steder, blir dette riktig template

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }


    @Test
    fun `TODO Skal kunne lagre og hente`() {

        val stillingsinfo = Testdata.enStillingsinfo

        val berikStillingerRequestBodyDto = BerikStillingerRequestBodyDto(
            stillinger = listOf(
                BerikStillingDto(
                    uuid = stillingsinfo.stillingsid.asString(),
                    expires = LocalDate.MIN
                )
            )
        )


        val url = "$localBaseUrl/indekser/berik_stillinger"
        val stillingsinfoRespons =
            restTemplate.exchange(url, HttpMethod.POST, httpEntity(berikStillingerRequestBodyDto), BerikStillingerRequestBodyDto::class.java)
        assertThat(stillingsinfoRespons.statusCode).isEqualTo(200)
    }

    private fun httpEntity(body: Any?): HttpEntity<Any> {
        val headers = mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE
        ).toMultiValueMap()
        return HttpEntity(body, headers)
    }
}