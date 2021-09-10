package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.Testdata.enStilling
import no.nav.rekrutteringsbistand.api.Testdata.enVeileder
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class StillingTest {

    @Mock
    lateinit var tokenUtils: TokenUtils

    @Test
    fun `Kopier stilling skal sette riktige verdier`() {
        `when`(tokenUtils.hentInnloggetVeileder()).thenReturn(enVeileder)

        val stilling = enStilling
        val kopiertStilling = stilling.toOpprettStillingDto(tokenUtils)

        assertThat(kopiertStilling.title).isEqualTo("Kopi - ${stilling.title}")
        assertThat(kopiertStilling.createdBy).isEqualTo("pam-rekrutteringsbistand")
        assertThat(kopiertStilling.updatedBy).isEqualTo("pam-rekrutteringsbistand")
        assertThat(kopiertStilling.source).isEqualTo("DIR")
        assertThat(kopiertStilling.privacy).isEqualTo("INTERNAL_NOT_SHOWN")
        assertThat(kopiertStilling.administration.status).isEqualTo("PENDING")
        assertThat(kopiertStilling.administration.reportee).isEqualTo("Clark Kent")
        assertThat(kopiertStilling.administration.navIdent).isEqualTo("C12345")

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
}
