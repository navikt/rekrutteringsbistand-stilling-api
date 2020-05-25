package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.RekrutterinsbistandStillingDto
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStilling
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enFjerdeStilling
import no.nav.rekrutteringsbistand.api.Testdata.enPage
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutterinsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutterinsbistandStillingUtenEier
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingUtenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillinggsinfoUtenEier
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enTredjeStilling
import no.nav.rekrutteringsbistand.api.Testdata.enTredjeStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.Eier
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.*
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
internal class StillingComponentTest {

    @get:Rule
    val wiremock = WireMockRule(9914)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(9924)

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/local/cookie-isso", String::class.java)
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)
        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/v1/stilling/${enStilling.uuid}", StillingMedStillingsinfo::class.java).also {
            assertThat(it).isEqualTo(enStilling)
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)
        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/rekrutteringsbistandstilling/${enStilling.uuid}", RekrutterinsbistandStillingDto::class.java).also {
            assertThat(it).isEqualTo(RekrutterinsbistandStillingDto(
                    stilling = enStilling.tilStilling(),
                    stillingsinfoid = null,
                    eier = null,
                    notat = null
            ))
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling med stillingsinfo hvis det er lagret`() {
        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)
        repository.lagre(enStillingsinfo)
        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/rekrutteringsbistandstilling/${enStillingsinfo.stillingsid}", RekrutterinsbistandStillingDto::class.java).also {
            assertThat(it).isEqualTo(RekrutterinsbistandStillingDto(
                    stilling = enStilling.tilStilling(),
                    stillingsinfoid = enStillingsinfo.stillingsinfoid.asString(),
                    eier = enStillingsinfo.eier,
                    notat = enStillingsinfo.notat
            ))
        }
    }



    @Test
    fun `GET mot en stilling skal returnere en stilling beriket med stillingsinfo`() {
        repository.lagre(enStillingsinfo)

        mockUtenAuthorization("/b2b/api/v1/ads/${enStilling.uuid}", enStilling)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/v1/stilling/${enStilling.uuid}", StillingMedStillingsinfo::class.java).also {
            assertThat(it.rekruttering).isEqualTo(enStillingsinfo.asEierDto())
            assertThat(it.uuid).isEqualTo(enStillingsinfo.stillingsid.asString())
        }
    }

    @Test
    fun `GET mot en stilling med stillingsnummer skal returnere en stilling beriket med stillingsinfo`() {
        repository.lagre(enStillingsinfo)

        mockUtenAuthorization("/b2b/api/v1/ads?id=1000", Page(listOf(enStilling), 1, 1))

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistand/api/v1/stilling/stillingsnummer/${enStilling.id}", StillingMedStillingsinfo::class.java).also {
            assertThat(it.rekruttering).isEqualTo(enStillingsinfo.asEierDto())
            assertThat(it.uuid).isEqualTo(enStillingsinfo.stillingsid.asString())
        }
    }

    @Test
    fun `GET mot stillinger skal returnere stillinger beriket med stillingsinfo`() {
        repository.lagre(enStillingsinfo)
        repository.lagre(enAnnenStillingsinfo)

        mock(HttpMethod.GET, "/api/v1/ads", enPage)

        val stillinger: List<StillingMedStillingsinfo> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        ).body!!.content

        assertThat(stillinger.first().rekruttering).isEqualTo(enStillingsinfo.asEierDto())
        assertThat(stillinger.last().rekruttering).isEqualTo(enAnnenStillingsinfo.asEierDto())
    }

    @Test
    fun `POST mot stillinger skal returnere stilling`() {

        mock(HttpMethod.POST, "/api/v1/ads", enStilling)
        mockKandidatlisteOppdatering()

        restTemplate.postForObject(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads",
                enStilling.copy(uuid = null),
                StillingMedStillingsinfo::class.java
        ).also {
            assertThat(it.uuid).isNotEmpty()
            assertThat(it.copy(uuid = null)).isEqualTo(enStilling.copy(uuid = null))
        }
    }

    @Test
    fun `PUT mot stilling skal returnere endret stilling`() {
        mock(HttpMethod.PUT, "/api/v1/ads/${enStilling.uuid}", enStilling)
        mockKandidatlisteOppdatering()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${enStilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(enStilling.copy(uuid = null)),
                StillingMedStillingsinfo::class.java
        ).body.also {
            assertThat(it!!.uuid).isNotEmpty()
            assertThat(it.copy(uuid = null)).isEqualTo(enStilling.copy(uuid = null))
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo finnes`() {
        mock(HttpMethod.PUT, "/api/v1/ads/${enRekrutterinsbistandStilling.stilling.uuid}", enTredjeStilling)
        mockKandidatlisteOppdatering()
        repository.lagre(enTredjeStillingsinfo.copy(notat = "gammelt notat"))

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/rekrutteringsbistandstilling/${enRekrutterinsbistandStilling.stilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(enRekrutterinsbistandStilling),
                RekrutterinsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty()
            assertThat(it.stilling.copy(uuid = null)).isEqualTo(enRekrutterinsbistandStilling.stilling.copy(uuid = null))
            assertThat(it.notat).isEqualTo(enRekrutterinsbistandStilling.notat)
            assertThat(it.eier).isEqualTo(enRekrutterinsbistandStilling.eier)
            assertThat(it.stillingsinfoid).isEqualTo(enRekrutterinsbistandStilling.stillingsinfoid)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke har eier`() {
        val rekrutteringsbistandStilling = enRekrutterinsbistandStillingUtenEier
        mock(HttpMethod.PUT, "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", enFjerdeStilling)
        mockKandidatlisteOppdatering()
        repository.lagre(enStillinggsinfoUtenEier.copy(notat = null))

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/rekrutteringsbistandstilling/${rekrutteringsbistandStilling.stilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(rekrutteringsbistandStilling),
                RekrutterinsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty()
            assertThat(it.stilling.copy(uuid = null)).isEqualTo(rekrutteringsbistandStilling.stilling.copy(uuid = null))
            assertThat(it.notat).isEqualTo(rekrutteringsbistandStilling.notat)
            assertThat(it.eier).isEqualTo(rekrutteringsbistandStilling.eier)
            assertThat(it.stillingsinfoid).isEqualTo(rekrutteringsbistandStilling.stillingsinfoid)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke finnes`() {
        val rekrutterindsbistandStilling = enRekrutterinsbistandStillingUtenEier
                .copy(stillingsinfoid = null)
        mock(HttpMethod.PUT, "/api/v1/ads/${rekrutterindsbistandStilling.stilling.uuid}", rekrutterindsbistandStilling.stilling)
        mockKandidatlisteOppdatering()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/rekrutteringsbistandstilling/${rekrutterindsbistandStilling.stilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(rekrutterindsbistandStilling),
                RekrutterinsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty()
            assertThat(it.stilling.copy(uuid = null)).isEqualTo(rekrutterindsbistandStilling.stilling.copy(uuid = null))
            assertThat(it.notat).isEqualTo(rekrutterindsbistandStilling.notat)
            assertThat(it.eier).isEqualTo(rekrutterindsbistandStilling.eier)
            assertThat(it.stillingsinfoid).isNotEmpty()
        }
    }

    @Test
    fun `PUT mot stilling med kandidatlistefeil skal returnere status 500`() {
        mock(HttpMethod.PUT, "/api/v1/ads/${enStilling.uuid}", enStilling)
        mockKandidatlisteOppdateringFeiler()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${enStilling.uuid}",
                HttpMethod.PUT,
                HttpEntity(enStilling.copy(uuid = null)),
                StillingMedStillingsinfo::class.java
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(it.hasBody()).isTrue()
            assertThat(it.body?.status).isEqualTo("500")
        }
    }

    @Test
    fun `DELETE mot stilling med kandidatlistefeil skal returnere status 500`() {
        val slettetStilling = enStillingUtenStillingsinfo.copy(status = "DELETED")
        mock(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdateringFeiler()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${enStillingUtenStillingsinfo.uuid}",
                HttpMethod.DELETE,
                HttpEntity(enStilling.copy(uuid = null)),
                Stilling::class.java
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(it.hasBody()).isTrue()
            assertThat(it.body?.status).isEqualTo("500")
        }

    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger uten stillingsinfo`() {
        mock(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", enPage)

        val respons: ResponseEntity<Page<StillingMedStillingsinfo>> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isEqualTo(enPage)
    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger med stillingsinfo`() {
        repository.lagre(enStillingsinfo)
        repository.lagre(enAnnenStillingsinfo)

        mock(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", enPage)

        val respons: ResponseEntity<Page<StillingMedStillingsinfo>> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/rekrutteringsbistand/minestillinger",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.content.first().rekruttering).isEqualTo(enStillingsinfo.asEierDto())
        assertThat(respons.body!!.content.last().rekruttering).isEqualTo(enAnnenStillingsinfo.asEierDto())
    }

    @Test
    fun `DELETE mot stilling skal returnere HTTP 200 med stilling og status DELETED`() {
        val slettetStilling = enStillingUtenStillingsinfo.copy(status = "DELETED")
        mock(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdatering()

        val respons: ResponseEntity<Stilling> = restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${slettetStilling.uuid}",
                HttpMethod.DELETE,
                null,
                Stilling::class.java
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isEqualTo(slettetStilling)
    }

    private fun mock(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremock.stubFor(
                request(method.name, urlPathMatching(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(AUTHORIZATION, matching("Bearer .*}"))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(responseBody)))
        )
    }

    private fun mockUtenAuthorization(urlPath: String, responseBody: Any) {
        wiremock.stubFor(
                get(urlEqualTo(urlPath))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse().withStatus(200)
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                                .withBody(objectMapper.writeValueAsString(responseBody)))
        )
    }

    private fun mockKandidatlisteOppdatering() {
        wiremockKandidatliste.stubFor(
                put(urlPathMatching("/pam-kandidatsok-api/rest/veileder/stilling/.*/kandidatliste"))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse().withStatus(HttpStatus.NO_CONTENT.value())
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        )
    }

    private fun mockKandidatlisteOppdateringFeiler() {
        wiremockKandidatliste.stubFor(
                put(urlPathMatching("/pam-kandidatsok-api/rest/veileder/stilling/.*/kandidatliste"))
                        .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                        .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                        .willReturn(aResponse()
                                .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                                .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
                        )

        )
    }

    @After
    fun tearDown() {
        repository.slett(enStillingsinfo.stillingsinfoid)
        repository.slett(enAnnenStillingsinfo.stillingsinfoid)
    }
}
