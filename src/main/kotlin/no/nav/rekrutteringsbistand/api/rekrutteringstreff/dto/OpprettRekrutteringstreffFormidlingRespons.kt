package no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto

import java.util.UUID

data class OpprettRekrutteringstreffFormidlingRespons(
    val kandidatlisteId: UUID,
    val stillingsId: UUID
)
