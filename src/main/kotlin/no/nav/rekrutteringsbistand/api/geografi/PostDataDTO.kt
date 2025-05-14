package no.nav.rekrutteringsbistand.api.geografi

data class PostDataDTO(
    val postkode: String,
    val by : String,
    val kommune: KommuneDTO,
    val fylke: FylkeDTO,
    val korrigertNavnBy: String
)
