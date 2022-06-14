package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.extensions.either.foldable.get
import arrow.core.left
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.matching.UrlPattern
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.TestRepository
import no.nav.rekrutteringsbistand.api.Testdata.enAnnenStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.Testdata.enOpprettetStilling
import no.nav.rekrutteringsbistand.api.Testdata.enPageMedStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.Testdata.enRekrutteringsbistandStillingUtenEier
import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfo
import no.nav.rekrutteringsbistand.api.Testdata.enStillingsinfoUtenEier
import no.nav.rekrutteringsbistand.api.config.MockLogin
import no.nav.rekrutteringsbistand.api.mockAzureObo
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
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
    val wiremockPamAdApi = WireMockRule(9934)

    @get:Rule
    val wiremockKandidatliste = WireMockRule(8766)

    @get:Rule
    val wiremockAzure = WireMockRule(9954)

    @LocalServerPort
    var port = 0

    val localBaseUrl by lazy { "http://localhost:$port" }

    @Autowired
    lateinit var repository: StillingsinfoRepository

    @Autowired
    lateinit var testRepository: TestRepository

    @Autowired
    lateinit var mockLogin: MockLogin

    private val restTemplate = TestRestTemplate()

    val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Before
    fun authenticateClient() {
        mockLogin.leggAzureVeilederTokenPåAlleRequests(restTemplate)
    }

    @Test
    fun `GET mot en stilling skal returnere en stilling uten stillingsinfo hvis det ikke er lagret`() {
        val stilling = enStilling
        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistandstilling/${stilling.uuid}", RekrutteringsbistandStilling::class.java).also {
            assertThat(it.stillingsinfo).isNull()
            assertThat(it.stilling).isEqualTo(stilling)
        }
    }

    @Test
    fun `GET mot en rekrutteringsbistandstilling skal returnere en stilling med stillingsinfo hvis det er lagret`() {

        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))

        mockUtenAuthorization("/b2b/api/v1/ads/${stilling.uuid}", stilling)
        mockAzureObo(wiremockAzure)

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
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))

        repository.opprett(stillingsinfo)

        mockUtenAuthorization("/b2b/api/v1/ads?id=1000", page)
        mockAzureObo(wiremockAzure)

        restTemplate.getForObject("$localBaseUrl/rekrutteringsbistandstilling/annonsenr/${stilling.id}", RekrutteringsbistandStilling::class.java).also {
            assertThat(it!!.stillingsinfo).isEqualTo(stillingsinfo.asStillingsinfoDto())
            assertThat(it.stilling).isEqualTo(stilling)
        }
    }

    @Test
    fun `POST mot stillinger skal returnere opprettet stilling`() {
        val rekrutteringsbistandStilling = enOpprettRekrutteringsbistandstillingDto

        mockPamAdApi(HttpMethod.POST, "/api/v1/ads", enOpprettetStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        restTemplate.postForObject(
            "$localBaseUrl/rekrutteringsbistandstilling",
            rekrutteringsbistandStilling,
            RekrutteringsbistandStilling::class.java
        ).also {
            val stilling = rekrutteringsbistandStilling.stilling
            assertThat(it!!.stilling.title).isEqualTo(stilling.title)
            assertThat(it.stilling.administration?.navIdent).isEqualTo(stilling.administration.navIdent)
            assertThat(it.stilling.administration?.reportee).isEqualTo(stilling.administration.reportee)
            assertThat(it.stilling.administration?.status).isEqualTo(stilling.administration.status)
            assertThat(it.stilling.createdBy).isEqualTo(stilling.createdBy)
            assertThat(it.stilling.updatedBy).isEqualTo(stilling.updatedBy)
            assertThat(it.stilling.source).isEqualTo(stilling.source)
            assertThat(it.stilling.privacy).isEqualTo(stilling.privacy)

            assertThat(it.stillingsinfo?.stillingskategori).isEqualTo(Stillingskategori.ARBEIDSTRENING)
        }
    }

    @Test
    fun `DELETE mot stillinger skal slette stilling`() {
        val slettetStilling = enStilling.copy(status = "DELETED")
        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdatering(::delete)
        mockAzureObo(wiremockAzure)

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling/${slettetStilling.uuid}",
            HttpMethod.DELETE,
            HttpEntity(null,null),
            Stilling::class.java
        ).also {
            val stilling = it.body
            assertThat(stilling?.title).isEqualTo(slettetStilling.title)
            assertThat(stilling?.administration?.navIdent).isEqualTo(slettetStilling.administration?.navIdent)
            assertThat(stilling?.administration?.reportee).isEqualTo(slettetStilling.administration?.reportee)
            assertThat(stilling?.administration?.status).isEqualTo(slettetStilling.administration?.status)
            assertThat(stilling?.createdBy).isEqualTo(slettetStilling.createdBy)
            assertThat(stilling?.updatedBy).isEqualTo(slettetStilling.updatedBy)
            assertThat(stilling?.source).isEqualTo(slettetStilling.source)
            assertThat(stilling?.privacy).isEqualTo(slettetStilling.privacy)
            assertThat(stilling?.status).isEqualTo("DELETED")
        }
    }

    @Test
    fun `PUT mot stilling skal returnere 500 og ikke gjøre endringer i databasen når kall mot Arbeidsplassen feiler`() {
        val stilling = enStilling
        val stillingsinfo = enStillingsinfo
        repository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockKandidatlisteOppdatering()
        mockPamAdApiError(HttpMethod.PUT, "/api/v1/ads/${stilling.uuid}", stilling)

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            notat = stillingsinfo.notat,
            stilling = stilling
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling",
            HttpMethod.PUT,
            HttpEntity(dto),
            String::class.java
        ).also {
            assertThat(it.statusCodeValue).isEqualTo(500)

            val lagretStillingsinfo = repository.hentForStilling(Stillingsid(stilling.uuid)).orNull()!!
            assertThat(lagretStillingsinfo.notat).isEqualTo(stillingsinfo.notat)
        }
    }

    @Test
    fun `PUT mot stilling skal returnere 500 og ikke gjøre endringer i database når kall mot kandidat-api feiler`() {
        val stilling = enOpprettetStilling
        val stillingsinfo = enStillingsinfo.copy(stillingsid = Stillingsid(stilling.uuid))
        repository.opprett(stillingsinfo)
        mockAzureObo(wiremockAzure)
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${stilling.uuid}", stilling)
        mockKandidatlisteOppdateringFeiler()

        val dto = OppdaterRekrutteringsbistandStillingDto(
            stillingsinfoid = stillingsinfo.stillingsinfoid.asString(),
            notat = "oppdatert notat",
            stilling = stilling
        )

        restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistandstilling",
            HttpMethod.PUT,
            HttpEntity(dto),
            String::class.java
        ).also {
            assertThat(it.statusCodeValue).isEqualTo(500)

            val lagretStillingsinfo = repository.hentForStilling(Stillingsid(stilling.uuid)).orNull()!!
            assertThat(lagretStillingsinfo.notat).isEqualTo(stillingsinfo.notat)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo finnes`() {

        val stilling = enStilling
        val stillingsinfo = enStillingsinfo.copy(notat = "gammelt notat")

        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${enRekrutteringsbistandStilling.stilling.uuid}", stilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

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

        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", rekrutteringsbistandStilling.stilling)
        mockAzureObo(wiremockAzure)

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
            assertThat(it!!.stilling.uuid).isNotEmpty
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling)
            assertThat(it.notat).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.notat)
            assertThat(it.stillingsinfoid).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.stillingsinfoid)
        }
    }

    @Test
    fun `PUT mot stilling med notat skal returnere endret stilling når stillingsinfo ikke finnes`() {
        val rekrutteringsbistandStilling = enRekrutteringsbistandStillingUtenEier
        mockPamAdApi(HttpMethod.PUT, "/api/v1/ads/${rekrutteringsbistandStilling.stilling.uuid}", rekrutteringsbistandStilling.stilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

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
            assertThat(it.stilling).isEqualTo(rekrutteringsbistandStilling.stilling)
            assertThat(it.notat).isEqualTo(rekrutteringsbistandStilling.stillingsinfo?.notat)
            assertThat(it.stillingsinfoid).isNotEmpty()
        }
    }

    @Test
    fun `DELETE mot stilling med kandidatlistefeil skal returnere status 500`() {
        val slettetStilling = enStilling.copy(status = "DELETED")

        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdateringFeiler()
        mockAzureObo(wiremockAzure)

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
        mockPamAdApi(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", enPageMedStilling)
        mockAzureObo(wiremockAzure)

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
        mockPamAdApi(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", page)
        mockAzureObo(wiremockAzure)

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
    fun `GET mot mine stillinger skal ikke returnere stillinger fra andre veiledere`() {
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

        mockPamAdApi(HttpMethod.GET, "/api/v1/ads/rekrutteringsbistand/minestillinger", page)
        mockAzureObo(wiremockAzure)
    }

    @Test
    fun `DELETE mot stilling skal returnere HTTP 200 med stilling og status DELETED`() {
        val slettetStilling = enStilling.copy(status = "DELETED")
        mockPamAdApi(HttpMethod.DELETE, "/api/v1/ads/${slettetStilling.uuid}", slettetStilling)
        mockKandidatlisteOppdatering()
        mockAzureObo(wiremockAzure)

        val respons: ResponseEntity<Stilling> = restTemplate.exchange(
            "$localBaseUrl/rekrutteringsbistand/api/v1/ads/${slettetStilling.uuid}",
            HttpMethod.DELETE,
            null,
            Stilling::class.java
        )

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body).isEqualTo(slettetStilling)
    }

    private fun mockPamAdApi(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            request(method.name, urlPathMatching(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(aResponse().withStatus(200)
                    .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(responseBody)))
        )
    }

    private fun mockPamAdApiError(method: HttpMethod, urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            request(method.name, urlPathMatching(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(AUTHORIZATION, matching("Bearer .*"))
                .willReturn(aResponse().withStatus(500)))
    }

    private fun mockUtenAuthorization(urlPath: String, responseBody: Any) {
        wiremockPamAdApi.stubFor(
            get(urlEqualTo(urlPath))
                .withHeader(CONTENT_TYPE, equalTo(APPLICATION_JSON_VALUE))
                .withHeader(ACCEPT, equalTo(APPLICATION_JSON_VALUE))
                .willReturn(aResponse().withStatus(200)
                    .withHeader(CONNECTION, "close") // https://stackoverflow.com/questions/55624675/how-to-fix-nohttpresponseexception-when-running-wiremock-on-jenkins
                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                    .withBody(objectMapper.writeValueAsString(responseBody)))
        )
    }

    private fun mockKandidatlisteOppdatering(metodeFunksjon: (UrlPattern)-> MappingBuilder = ::put) {
        wiremockKandidatliste.stubFor(
            metodeFunksjon(urlPathMatching("/rekrutteringsbistand-kandidat-api/rest/veileder/stilling/.*/kandidatliste"))
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
