package no.nav.rekrutteringsbistand.api.kandidatliste

import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto

data class KandidatlisteDto(
    val stillingsinfo: StillingsinfoDto?,
    val stilling: KandidatlisteStillingDto
)
