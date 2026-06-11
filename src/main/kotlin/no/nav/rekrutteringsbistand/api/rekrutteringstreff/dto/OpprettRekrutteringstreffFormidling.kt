package no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettRekrutteringstreffFormidling(
    val eierNavKontorEnhetId: String,
    val rekrutteringstreffId: UUID,
    val stilling: RekrutteringstreffStilling
)