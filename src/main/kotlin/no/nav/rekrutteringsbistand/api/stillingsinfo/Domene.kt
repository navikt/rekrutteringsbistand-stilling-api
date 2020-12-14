package no.nav.rekrutteringsbistand.api.stillingsinfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.sql.ResultSet
import java.util.*

data class Stillingsinfo(
        val stillingsinfoid: Stillingsinfoid,
        val stillingsid: Stillingsid,
        val eier: Eier?,
        val notat: String?
) {

    fun asEierDto() = EierDto(
        stillingsinfoid = this.stillingsinfoid.toString(),
        stillingsid = this.stillingsid.toString(),
        eierNavident = this.eier?.navident,
        eierNavn = this.eier?.navn
    )

    fun asStillingsinfoDto() = StillingsinfoDto(
        stillingsid = this.stillingsid.asString(),
        stillingsinfoid = this.stillingsinfoid.toString(),
        notat = this.notat,
        eierNavident = this.eier?.navident,
        eierNavn = this.eier?.navn
    )

    companion object {
        fun fromDB(rs: ResultSet) =
                Stillingsinfo(
                        stillingsinfoid = Stillingsinfoid(verdi = rs.getString("stillingsinfoid")),
                        stillingsid = Stillingsid(verdi = rs.getString("stillingsid")),
                        eier = if(rs.getString("eier_navident") == null) null else Eier(navident = rs.getString("eier_navident"), navn = rs.getString("eier_navn")),
                        notat = rs.getString("notat")
                )
    }
}

data class OppdaterEier(val stillingsinfoid: Stillingsinfoid, val eier: Eier)

data class OppdaterNotat(val stillingsinfoid: Stillingsinfoid, val notat: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EierDto(
        val stillingsinfoid: String? = null,
        val stillingsid: String,
        val eierNavident: String?,
        val eierNavn: String?
) {
    fun asStillinginfo() =
            Stillingsinfo(
                    stillingsinfoid = Stillingsinfoid(verdi = this.stillingsinfoid
                            ?: throw IllegalArgumentException("Stillingsinfo må ha en stillingsinfoid")
                    ),
                    stillingsid = Stillingsid(verdi = this.stillingsid),
                    eier = Eier(navident = this.eierNavident, navn = this.eierNavn),
                    notat = null
            )

    fun asOppdaterEierinfo() =
            OppdaterEier(
                    stillingsinfoid = Stillingsinfoid(verdi = this.stillingsinfoid
                            ?: throw IllegalArgumentException("Stillingsinfo må ha en stillingsinfoid")
                    ),
                    eier = Eier(navident = this.eierNavident, navn = this.eierNavn)
            )
}

data class Stillingsinfoid(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString() = verdi.toString()
    fun asUuid() = verdi
    override fun toString(): String = asString()
}

data class Stillingsid(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString() = verdi.toString()
    fun asUuid() = verdi
    override fun toString(): String = asString()
}

data class Eier(val navident: String?, val navn: String?)

data class StillingsinfoDto(
        val stillingsid: String,
        val stillingsinfoid: String,
        val notat: String?,
        val eierNavident: String?,
        val eierNavn: String?
)
