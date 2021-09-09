package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettStillingDto
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettetStilling
import no.nav.rekrutteringsbistand.api.Testdata.enPageMedStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStillingUtenEier
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoUtenEier
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.*
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
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class StillingComponentTest {

    @get:Rule
    val wiremock = WireMockRule(9914)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(8766)

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var testRepository: TestRepository

    private val restTemplate = TestRestTemplate(TestRestTemplate.HttpClientOption.ENABLE_COOKIES)

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/veileder-token-cookie", Unit::class.java)
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        val stilling = enStilling
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java).also {
            assertThat(it.stillingsinfo).isNull()
            assertThat(it.stilling).isEqualTo(stilling)
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling med stillingsinfo hvis det er lagret`() {

        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid!!))

        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        repository.opprett(stillingsinfo)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java).also {
            assertThat(it.stilling).isEqualTo(stilling)
            assertThat(it.stillingsinfo).isEqualTo(stillingsinfo.asStillingsinfoDto())
        }
    }


    @Test
    fun `GET mot en stilling med annonsenummer skal returnere en stilling beriket med stillingsinfo`() {

        val stilling = enStilling
        val page = Page(
            content = listOf(stilling),
            totalPages = 1,
            totalElements = 1
        )
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid!!))

        repository.opprett(stillingsinfo)

        mockUtenAuthorization("/b2b/api/v1/ads?id=1000", page)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistandstilling/annonsenr/${stilling.id}", RekrutteringsbistandStilling::class.java).also {
            assertThat(it.stillingsinfo).isEqualTo(stillingsinfo.asStillingsinfoDto())
            assertThat(it.stilling).isEqualTo(stilling)
        }
    }

    @Test
    fun `POST mot stillinger skal returnere opprettet stilling`() {
        val stilling = enOpprettStillingDto

        mock(HttpMethod.POST, "/api/v1/ads", enOpprettetStilling)
        mockKandidatlisteOppdatering()

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            stilling,
            RekrutteringsbistandStilling::class.java
        ).also {
            assertThat(it.stilling.uuid).isNotNull
            assertThat(it.stilling.id).isNotNull

            assertThat(it.stilling.title).isEqualTo(stilling.title)
            assertThat(it.stilling.administration?.navIdent).isEqualTo(stilling.administration.navIdent)
            assertThat(it.stilling.administration?.reportee).isEqualTo(stilling.administration.reportee)
            assertThat(it.stilling.createdBy).isEqualTo(stilling.createdBy)
            assertThat(it.stilling.updatedBy).isEqualTo(stilling.updatedBy)
            assertThat(it.stilling.source).isEqualTo(stilling.source)
            assertThat(it.stilling.privacy).isEqualTo(stilling.privacy)
            assertThat(it.stillingsinfo).isNull()
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo finnes`() {

        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(notat = "gammelt notat")

        mock(HttpMethod.PUT, "/api/v1/ads/${enRekrutteringsbistandStilling.stilling.uuid}", stilling)
        mockKandidatlisteOppdatering()

        repository.opprett(stillingsinfo)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            notat = stillingsinfo.notat,
            stilling = stilling
        )

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistandstilling",
                HttpMethod.PUT,
                HttpEntity(dto),
                OppdaterRekrutteringsbistandStillingDto::class.java
        ).body!!.also {
            assertThat(it.stilling).isEqualTo(stilling)
            assertThat(it.notat).isEqualTo(stillingsinfo.notat)
            assertThat(it.stillingsinfoid).isEqualTo(stillingsinfo.stillingsinfoid.asString())
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke har eier`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStillingUtenEier

        mock(HttpMethod.PUT, "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", rekrutteringsbistandStilling.stilling)

        mockKandidatlisteOppdatering()
        repository.opprett(enStillingsinfoUtenEier.copy(notat = null))

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistandstilling",
                HttpMethod.PUT,
                HttpEntity(OppdaterRekrutteringsbistandStillingDto(
                        stillingsinfoid = rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid,
                        notat = rekrutteringsbistandStilling.stillingsinfo?.notat,
                        stilling = rekrutteringsbistandStilling.stilling
                )),
                OppdaterRekrutteringsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty()
            assertThat(it.stilling.copy(uuid = null)).isEqualTo(rekrutteringsbistandStilling.stilling.copy(uuid = null))
            assertThat(it.notat).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.notat)
            assertThat(it.stillingsinfoid).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke finnes`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStillingUtenEier
        mock(HttpMethod.PUT, "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", rekrutteringsbistandStilling.stilling)
        mockKandidatlisteOppdatering()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistandstilling",
                HttpMethod.PUT,
                HttpEntity(OppdaterRekrutteringsbistandStillingDto(
                        stillingsinfoid = rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid,
                        notat = rekrutteringsbistandStilling.stillingsinfo?.notat,
                        stilling = rekrutteringsbistandStilling.stilling
                )),
                OppdaterRekrutteringsbistandStillingDto::class.java
        ).body.also {
            assertThat(it!!.stilling.uuid).isNotEmpty()
            assertThat(it.stilling.copy(uuid = null)).isEqualTo(rekrutteringsbistandStilling.stilling.copy(uuid = null))
            assertThat(it.notat).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.notat)
            assertThat(it.stillingsinfoid).isNotEmpty()
        }
    }

    @Test
    fun `DELETE mot stilling med kandidatlistefeil skal returnere status 500`() {
        val slettetStilling = enStilling.copy(status = "DELETED")

        mock(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdateringFeiler()

        restTemplate.exchange(
                "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${enStilling.uuid}",
                HttpMethod.DELETE,
                null,
                Stilling::class.java
        ).also {
            assertThat(it.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger uten stillingsinfo`() {
        mock(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", enPageMedStilling)

        val respons = restTemplate.exchange(
                "$localBaseUrl/mine-stillinger",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<RekrutteringsbistandStilling>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        respons.body!!.content.forEachIndexed { index, rekrutteringsbistandStilling ->
            assertThat(rekrutteringsbistandStilling.stilling).isEqualTo(enPageMedStilling.content[index])
            assertThat(rekrutteringsbistandStilling.stillingsinfo).isNull()
        }
    }

    @Test
    fun `GET mot mine stillinger skal returnere HTTP 200 med mine stillinger med stillingsinfo`() {

        val stillingsinfo1 = enStillingsinfo
        val stillingsinfo2 = enAnnenStillingsinfo

        val stilling1 = enStilling.copy(uuid = stillingsinfo1.stillingsid.asString())
        val stilling2 = enStilling.copy(uuid = stillingsinfo2.stillingsid.asString())

        repository.opprett(stillingsinfo1)
        repository.opprett(stillingsinfo2)

        val page = Page(
            content = listOf(stilling1, stilling2),
            totalElements = 2,
            totalPages = 1
        )
        mock(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", page)

        val respons = restTemplate.exchange(
                "$localBaseUrl/mine-stillinger",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Page<RekrutteringsbistandStilling>>() {}
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body!!.content.first().stillingsinfo).isEqualTo(stillingsinfo1.asStillingsinfoDto())
        assertThat(respons.body!!.content.last().stillingsinfo).isEqualTo(stillingsinfo2.asStillingsinfoDto())
    }

    @Test
    fun `DELETE mot stilling skal returnere HTTP 200 med stilling og status DELETED`() {
        val slettetStilling = enStilling.copy(status = "DELETED")
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

    @Ignore
    @Test
    fun `POST mot kopier-stilling skal returnere kopi av rekrutteringsbistandstilling med gitt stillingsId`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStilling
        val stilling = rekrutteringsbistandStilling.stilling

        val respons = restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/kopier/${stilling.uuid}",
            HttpMethod.POST,
            null,
            RekrutteringsbistandStilling::class.java
        )

        val kopiertStilling = respons.body!!.stilling

        assertThat(kopiertStilling.title).isEqualTo("Kopi - ${stilling.title}")
        assertThat(kopiertStilling.createdBy).isEqualTo("pam-rekrutteringsbistand")
        assertThat(kopiertStilling.updatedBy).isEqualTo("pam-rekrutteringsbistand")
        assertThat(kopiertStilling.source).isEqualTo("DIR")
        assertThat(kopiertStilling.privacy).isEqualTo("INTERNAL_NOT_SHOWN")
        assertThat(kopiertStilling.administration?.status).isEqualTo("PENDING")
        assertThat(kopiertStilling.administration?.reportee).isEqualTo("Clark Kent")
        assertThat(kopiertStilling.administration?.navIdent).isEqualTo("C12345")

        assertThat(kopiertStilling.medium).isEqualTo(stilling.medium)
        assertThat(kopiertStilling.employer).isEqualTo(stilling.employer)
        assertThat(kopiertStilling.location).isEqualTo(stilling.location)
        assertThat(kopiertStilling.locationList).isEqualTo(stilling.locationList)
        assertThat(kopiertStilling.properties).isEqualTo(stilling.properties)
        assertThat(kopiertStilling.businessName).isEqualTo(stilling.businessName)
        assertThat(kopiertStilling.deactivatedByExpiry).isEqualTo(stilling.deactivatedByExpiry)
        assertThat(kopiertStilling.categoryList).isEqualTo(stilling.categoryList)
        assertThat(kopiertStilling.activationOnPublishingDate).isEqualTo(stilling.activationOnPublishingDate)
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
            put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(aResponse().withStatus(HttpStatus.NO_CONTENT.value())
                    .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        )
    }

    private fun mockKandidatlisteOppdateringFeiler() {
        wiremockKandidatliste.stubFor(
            put(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.+/kandidatliste"))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(aResponse().withStatus(500)
                    .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                )
        )
    }

    @After
    fun tearDown() {
        testRepository.slettAlt()
    }
}
