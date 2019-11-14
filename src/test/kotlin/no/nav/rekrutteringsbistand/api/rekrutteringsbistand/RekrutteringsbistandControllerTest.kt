package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.TestRestTemplate.HttpClientOption.ENABLE_COOKIES
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class RekrutteringsbisandComponentTest {

    @LocalServerPort
    private var port = 0

    val localBaseUrl by lazy { "http://localhost:$port/rekrutteringsbistand-api" }

    val restTemplate = TestRestTemplate(ENABLE_COOKIES)

    @Before
    fun authenticateClient() {
        restTemplate.getForObject("$localBaseUrl/local/cookie-isso", String::class.java)
    }

    @Test
    fun `Lagring av rekrutteringsbistand skal returnere HTTP status 201 og JSON med nyopprettet rekrutteringUuid`() {
        // Given
        val url = "$localBaseUrl/rekruttering"
        val input = RekrutteringsbistandDto(
                eierIdent = "anyEierident",
                eierNavn = "anyEierNavn",
                stillingUuid = "anyStillingUuid")
        val headers = mapOf(
                CONTENT_TYPE to APPLICATION_JSON.toString(),
                ACCEPT to APPLICATION_JSON.toString()
        ).toMultiValueMap()
        val request = HttpEntity(input, headers)

        // When
        val actualResponse = restTemplate.postForEntity(url, request, RekrutteringsbistandDto::class.java)

        // Then
        assertThat(actualResponse.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(actualResponse.hasBody())
        actualResponse.body!!.apply {
            assertThat(rekrutteringUuid).isNotBlank()
            assertDoesNotThrow { UUID.fromString(rekrutteringUuid) }
            assertThat(stillingUuid).isEqualTo(input.stillingUuid)
            assertThat(eierIdent).isEqualTo(input.eierIdent)
            assertThat(eierNavn).isEqualTo(input.eierNavn)
        }
    }
}