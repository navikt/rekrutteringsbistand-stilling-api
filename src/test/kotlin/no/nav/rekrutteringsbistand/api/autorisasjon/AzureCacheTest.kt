package no.nav.rekrutteringsbistand.api.autorisasjon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AzureCacheTest {
    private val appScope = "app_scope"
    private val navIdent = "nav_ident"
    private lateinit var azureCache: AzureCache

    @BeforeEach
    fun initializeCache() {
        azureCache = AzureCache()
    }

    @Test
    fun `Cache skal initialiseres tom`() {
        assertThat(azureCache.hentOBOToken(appScope, navIdent)).isNull()
    }

    @Test
    fun `Cache skal treffe hvis det finnes et token som ikke er utløpt`() {
        azureCache.lagreOBOToken(appScope, navIdent, AzureResponse("token", 60))

        val cachedToken = azureCache.hentOBOToken(appScope, navIdent)

        assertThat(cachedToken).isEqualTo("token")
    }

    @Test
    fun `Cache skal returnere null hvis cachet token er utløpt`() {
        azureCache.lagreOBOToken(appScope, navIdent, AzureResponse("token", -1))
        azureCache.lagreOBOToken(appScope, "enAnnenBruker", AzureResponse("token", 60))

        val cachedToken = azureCache.hentOBOToken(appScope, navIdent)

        assertThat(cachedToken).isNull()
    }

    @Test
    fun `Cache skal returnere null hvis cachet token utløper om mindre enn 10 sekunder`() {
        azureCache.lagreOBOToken(appScope, navIdent, AzureResponse("token", 9))

        val cachedToken = azureCache.hentOBOToken(appScope, navIdent)

        assertThat(cachedToken).isNull()
    }
}
