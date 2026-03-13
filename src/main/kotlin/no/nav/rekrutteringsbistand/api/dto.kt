package no.nav.rekrutteringsbistand.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.FrontendStilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto

@JsonIgnoreProperties(ignoreUnknown = true)
data class RekrutteringsbistandStilling(
    val stillingsinfo: StillingsinfoDto?,
    val stilling: FrontendStilling
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KopierStillingDto(
    val eierNavKontorEnhetId: String?,
)