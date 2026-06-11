package no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto

import no.nav.rekrutteringsbistand.api.stilling.Arbeidsgiver
import no.nav.rekrutteringsbistand.api.stilling.Geografi
import no.nav.rekrutteringsbistand.api.stilling.Kategori

data class RekrutteringstreffStilling(
    val employer: Arbeidsgiver,
    val locationList: List<Geografi>,
    val categoryList: List<Kategori>,
    val properties: Map<String, String> = HashMap(),
)
