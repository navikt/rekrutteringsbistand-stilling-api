package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.geografi.FylkeDTO
import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.geografi.KommuneDTO
import no.nav.rekrutteringsbistand.api.geografi.PostDataDTO
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class FrontendStillingServiceTest {
    @Mock
    lateinit var stillingsinfoService: StillingsinfoService
    @Mock
    lateinit var tokenUtils: TokenUtils
    @Mock
    lateinit var kandidatlisteKlient: KandidatlisteKlient
    @Mock
    lateinit var arbeidsplassenKlient: ArbeidsplassenKlient
    @Mock
    lateinit var direktemeldtStillingService: DirektemeldtStillingService
    @Mock
    lateinit var stillingssokProxyClient: StillingssokProxyClient
    @Mock
    lateinit var geografiService: GeografiService
    @Mock
    lateinit var stillingOutboxService: StillingOutboxService


    lateinit var stillingService: StillingService

    @BeforeEach
    fun setUp() {
        stillingService = StillingService(
            stillingsinfoService,
            tokenUtils,
            kandidatlisteKlient,
            arbeidsplassenKlient,
            direktemeldtStillingService,
            stillingssokProxyClient,
            geografiService,
            stillingOutboxService,
        )
    }

    @Test
    fun `Geografi er populert med fylke og land hvis kommune er satt`() {
        whenever(geografiService.finnPostdataFraKommune(null, "Oslo")).thenReturn(PostDataDTO(
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
        )
        )

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

        val populertGeografi = stillingService.populerGeografi(upopulertGeografi)

        assertEquals("OSLO", populertGeografi?.county)
        assertEquals("NORGE", populertGeografi?.country)
    }


    @Test
    fun `Geografi er populert hvis postnummer er satt`() {
        whenever(geografiService.finnPostdata("5678")).thenReturn(PostDataDTO(
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

        val populertGeografi = stillingService.populerGeografi(upopulertGeografi)

        assertEquals("HORDALAND", populertGeografi?.county)
        assertEquals("NORGE", populertGeografi?.country)
        assertEquals("1201", populertGeografi?.municipalCode)
        assertEquals("BERGEN", populertGeografi?.municipal)
        assertEquals("BERGEN", populertGeografi?.city)
    }
}
