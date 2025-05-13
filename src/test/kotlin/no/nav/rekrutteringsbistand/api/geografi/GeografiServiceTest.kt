package no.nav.rekrutteringsbistand.api.geografi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class GeografiServiceTest {

    @Mock
    lateinit var geografiKlient: GeografiKlient

    lateinit var geografiService: GeografiService

    @BeforeEach
    fun setUp() {
        geografiService = GeografiService(geografiKlient)    }

    @Test
    fun `Test at fylke blir returert hvis kommunenr finnes`() {

        val postData = listOf(
            PostDataDTO(
                postkode = "1234",
                by = "OSLO",
                kommune = KommuneDTO(
                    kommunenummer = "0301",
                    navn = "OSLO",
                    fylkesnummer = "03",
                    korrigertNavn = "Oslo"
                ),
                fylke = FylkeDTO(
                    fylkesnummer = "03",
                    navn = "OSLO",
                    korrigertNavn = "Oslo"
                ),
                korrigertNavnBy = "Oslo"
            ),
            PostDataDTO(
                postkode = "5678",
                by = "BERGEN",
                kommune = KommuneDTO(
                    kommunenummer = "1201",
                    navn = "BERGEN",
                    fylkesnummer = "12",
                    korrigertNavn = "Bergen"
                ),
                fylke = FylkeDTO(
                    fylkesnummer = "12",
                    navn = "HORDALAND",
                    korrigertNavn = "Hordaland"
                ),
                korrigertNavnBy = "Bergen"
            )
        )

        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)

        val fylke = geografiService.finnFylke("1201")

        assertEquals("HORDALAND", fylke)
    }


}
