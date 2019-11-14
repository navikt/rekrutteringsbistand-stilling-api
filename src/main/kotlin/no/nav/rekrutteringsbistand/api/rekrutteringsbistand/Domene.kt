package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.sql.ResultSet

data class Rekrutteringsbistand(
        val rekrutteringUuid: String?,
        val stillingUuid: String,
        val eierIdent: String,
        val eierNavn: String
) {

    fun asDto() =
            RekrutteringsbistandDto(
                    rekrutteringUuid = this.rekrutteringUuid,
                    stillingUuid = this.stillingUuid,
                    eierIdent = this.eierIdent,
                    eierNavn = this.eierNavn)

    companion object {
        fun fromDB(rs: ResultSet) =
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        eierIdent = rs.getString("eier_ident"),
                        eierNavn = rs.getString("eier_navn"))
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RekrutteringsbistandDto(
        val rekrutteringUuid: String? = null,
        val stillingUuid: String,
        val eierIdent: String,
        val eierNavn: String
) {
    fun asRekrutteringsbistand() =
            Rekrutteringsbistand(
                    rekrutteringUuid = this.rekrutteringUuid,
                    stillingUuid = this.stillingUuid,
                    eierIdent = this.eierIdent,
                    eierNavn = this.eierNavn
            )
}
