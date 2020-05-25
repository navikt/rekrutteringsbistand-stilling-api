package no.nav.rekrutteringsbistand.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto

@JsonIgnoreProperties(ignoreUnknown = true)
data class HentRekrutterinsbistandStillingDto(
        val stilingsinfo: StillingsinfoDto?,
        val stilling: Stilling
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OppdaterRekrutterinsbistandStillingDto(
        val stillingsinfoid: String?,
        val stilling: Stilling,
        val notat: String?
)