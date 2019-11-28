package no.nav.rekrutteringsbistand.api.kandidatliste

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class KandidatlisteKlientTest {

    @get:Rule
    val wiremock = WireMockRule(WireMockConfiguration.options().port(9924))

    @Autowired
    lateinit var klient: KandidatlisteKlient

    @Test
    fun `oppdaterKandidatliste skal returnere no content`() {
        wiremock.stubFor(
                put(urlPathMatching("/pam-kandidatsok-api/rest/veileder/stilling/.*/kandidatliste"))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON.toString()))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON.toString()))
                        .willReturn(aResponse().withStatus(NO_CONTENT.value())
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        )

        val respons = klient.oppdaterKandidatliste(Stillingsid(UUID.randomUUID()))
        assertThat(respons.statusCode).isEqualTo(NO_CONTENT)
    }
}
