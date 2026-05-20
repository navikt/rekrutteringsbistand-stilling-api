package no.nav.rekrutteringsbistand.api.geografi

import no.nav.rekrutteringsbistand.api.stilling.Geografi
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
        geografiService = GeografiService(geografiKlient)
    }
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


    @Test
    fun `Test at fylke blir returnert hvis fylke finnes`() {
        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)

        val fylke = geografiService.finnFylke("hordaland")

        assertEquals("HORDALAND", fylke)
    }

    @Test
    fun `Test at postdata blir returnert for kommunenummer`() {
        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)
        val postData = geografiService.finnPostdataFraKommune("0301", null)
        assertEquals("OSLO", postData?.kommune?.navn)
        assertEquals("OSLO", postData?.fylke?.navn)

    }

    @Test
    fun `Test at postdata blir returnert for kommunenavn`() {
        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)

        val postData = geografiService.finnPostdataFraKommune(null, "bergen")
        assertEquals("1201", postData?.kommune?.kommunenummer)
        assertEquals("HORDALAND", postData?.fylke?.navn)
    }

    @Test
    fun `Test at geografi blir populert hvis postnummer er satt `() {
        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)

        val upopulertGeografi = Geografi(
            address = null,
            municipalCode = null,
            city = null,
            county = null,
            municipal = null,
            country = null,
            postalCode = "5678",
            latitude = null,
            longitude = null
        )

        val populertGeografi = geografiService.populerGeografi(upopulertGeografi)

        assertEquals("HORDALAND", populertGeografi?.county)
        assertEquals("NORGE", populertGeografi?.country)
        assertEquals("1201", populertGeografi?.municipalCode)
        assertEquals("BERGEN", populertGeografi?.municipal)
        assertEquals("BERGEN", populertGeografi?.city)
    }

    @Test
    fun `Test at geografi blir populert med fylke og land hvis kommune er satt`() {
        whenever(geografiKlient.hentAllePostdata()).thenReturn(postData)

        val upopulertGeografi = Geografi(
            address = null,
            municipalCode = null,
            city = null,
            county = null,
            municipal = "Oslo",
            country = null,
            postalCode = null,
            latitude = null,
            longitude = null
        )

        val populertGeografi = geografiService.populerGeografi(upopulertGeografi)

        assertEquals("OSLO", populertGeografi?.county)
        assertEquals("NORGE", populertGeografi?.country)    }
}
