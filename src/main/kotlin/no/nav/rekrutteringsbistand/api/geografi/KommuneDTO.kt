package no.nav.rekrutteringsbistand.api.geografi

data class KommuneDTO(
    val kommunenummer: String,
    val navn: String,
    val fylkesnummer: String,
    val korrigertNavn: String
)
