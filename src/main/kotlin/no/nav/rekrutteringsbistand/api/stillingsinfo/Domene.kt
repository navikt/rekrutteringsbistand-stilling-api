package no.nav.rekrutteringsbistand.api.stillingsinfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.lang.IllegalArgumentException
import java.sql.ResultSet
import java.util.*

data class Stillingsinfo(
        val stillingsinfoid: Stillingsinfoid,
        val stillingsid: Stillingsid,
        val eier: Eier
) {

    fun asDto() =
            StillingsinfoDto(
                    stillingsinfoid = this.stillingsinfoid.toString(),
                    stillingsid = this.stillingsid.toString(),
                    eierNavident = this.eier.navident,
                    eierNavn = this.eier.navn)

    companion object {
        fun fromDB(rs: ResultSet) =
                Stillingsinfo(
                        stillingsinfoid = Stillingsinfoid(verdi = rs.getString("stillingsinfoid")),
                        stillingsid = Stillingsid(verdi = rs.getString("stillingsid")),
                        eier = Eier(navident = rs.getString("eier_navident"), navn = rs.getString("eier_navn")))
    }
}


data class OppdaterStillingsinfo(val stillingsinfoid: Stillingsinfoid, val eier: Eier)

@JsonIgnoreProperties(ignoreUnknown = true)
data class StillingsinfoDto(
        val stillingsinfoid: String? = null,
        val stillingsid: String,
        val eierNavident: String,
        val eierNavn: String
) {
    fun asStillingsinfo() =
            Stillingsinfo(
                    stillingsinfoid = Stillingsinfoid(verdi = this.stillingsinfoid
                            ?: throw IllegalArgumentException("Stillingsinfo må ha en stillingsinfoid")
                    ),
                    stillingsid = Stillingsid(verdi = this.stillingsid),
                    eier = Eier(navident = this.eierNavident, navn = this.eierNavn)
            )

    fun asOppdaterStillingsinfo() =
            OppdaterStillingsinfo(
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

data class Eier(val navident: String, val navn: String)
