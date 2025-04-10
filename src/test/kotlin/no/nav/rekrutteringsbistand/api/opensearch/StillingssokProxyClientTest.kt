package no.nav.rekrutteringsbistand.api.opensearch

import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import no.nav.rekrutteringsbistand.api.StillingssokProxyMock
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.mockAzureObo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StillingssokProxyClientTest {

    val stillingssokProxyMock: StillingssokProxyMock = StillingssokProxyMock()

    companion object {
        @JvmStatic
        @RegisterExtension
        val wiremockAzure: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9954))
            .build()

        @JvmStatic
        @RegisterExtension
        val wiremockStillingssokProxyClient: WireMockExtension = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().port(9937).notifier(Slf4jNotifier(true)))
            .build()
    }

    @MockitoBean
    lateinit var tokenUtils: TokenUtils

    @Autowired
    lateinit var stillingssokProxyClient: StillingssokProxyClient

    @Test
    fun `Sjekk at responsen fra openSearch blir mappet riktig`() {
        whenever(tokenUtils.hentOBOToken("placeholder")).thenReturn("eksempeltoken")

        mockAzureObo(wiremockAzure)
        stillingssokProxyMock.mockStillingssokProxy(wiremockStillingssokProxyClient, "/stilling/_doc/4f7417d0-8678-4b75-9536-ec94cc4aa5bf")

        val stilling = stillingssokProxyClient.hentStilling("4f7417d0-8678-4b75-9536-ec94cc4aa5bf")

        assertEquals("4f7417d0-8678-4b75-9536-ec94cc4aa5bf", stilling.uuid)
        val expected = """
           ["Dagtid","Kveld"]
        """.trimIndent()

        assertEquals(expected, stilling.properties["workhours"])
    }
}
