package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.lang.IllegalArgumentException
import java.sql.ResultSet
import java.util.*

data class Rekrutteringsbistand(
        val rekrutteringId: RekrutteringId,
        val stillingId: StillingId,
        val eier: Eier
) {

    fun asDto() =
            RekrutteringsbistandDto(
                    rekrutteringUuid = this.rekrutteringId.toString(),
                    stillingUuid = this.stillingId.toString(),
                    eierIdent = this.eier.ident,
                    eierNavn = this.eier.navn)

    companion object {
        fun fromDB(rs: ResultSet) =
                Rekrutteringsbistand(
                        rekrutteringId = RekrutteringId(verdi = rs.getString("rekruttering_uuid")),
                        stillingId = StillingId(verdi = rs.getString("stilling_uuid")),
                        eier = Eier(ident = rs.getString("eier_ident"), navn = rs.getString("eier_navn")))
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
                    rekrutteringId = RekrutteringId(verdi = this.rekrutteringUuid
                            ?: throw IllegalArgumentException("Rekrutteringsbistand må ha en uuid")
                    ),
                    stillingId = StillingId(verdi = this.stillingUuid),
                    eier = Eier(ident = this.eierIdent, navn = this.eierNavn)
            )
}

data class RekrutteringId(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString() = verdi.toString()
    fun asUuid() = verdi
    override fun toString(): String = asString()
}

data class StillingId(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString() = verdi.toString()
    fun asUuid() = verdi
    override fun toString(): String = asString()
}

data class Eier(val ident: String, val navn: String)
