package no.nav.rekrutteringsbistand.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.Eier

@JsonIgnoreProperties(ignoreUnknown = true)
data class RekrutterinsbistandStillingDto(
        val stillingsinfoid: String?,
        val stilling: Stilling,
        val notat: String?,
        val eier: Eier?
)