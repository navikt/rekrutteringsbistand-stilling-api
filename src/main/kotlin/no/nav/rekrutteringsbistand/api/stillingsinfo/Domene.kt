package no.nav.rekrutteringsbistand.api.stillingsinfo

import java.sql.ResultSet
import java.util.*

data class Stillingsinfo(
    val stillingsinfoid: Stillingsinfoid,
    val stillingsid: Stillingsid,
    val eier: Eier?,
    val stillingskategori: Stillingskategori? = Stillingskategori.STILLING
) {
    fun asStillingsinfoDto() = StillingsinfoDto(
        stillingsid = this.stillingsid.asString(),
        stillingsinfoid = this.stillingsinfoid.toString(),
        eierNavident = this.eier?.navident,
        eierNavn = this.eier?.navn,
        stillingskategori = this.stillingskategori
    )

    companion object {
        fun fromDB(rs: ResultSet) =
            Stillingsinfo(
                stillingsinfoid = Stillingsinfoid(verdi = rs.getString("stillingsinfoid")),
                stillingsid = Stillingsid(verdi = rs.getString("stillingsid")),
                eier = if (rs.getString("eier_navident") == null) null else Eier(
                    navident = rs.getString("eier_navident"),
                    navn = rs.getString("eier_navn")
                ),
                stillingskategori = Stillingskategori.fraDatabase(rs.getString("stillingskategori"))
            )
    }
}

data class OppdaterEier(val stillingsinfoid: Stillingsinfoid, val eier: Eier)

data class Stillingsinfoid(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString() = verdi.toString()
    fun asUuid() = verdi
    override fun toString(): String = asString()

    companion object {
        fun ny() = Stillingsinfoid(UUID.randomUUID())
    }
}

data class Stillingsid(val verdi: UUID) {
    constructor(verdi: String) : this(UUID.fromString(verdi))

    fun asString(): String = verdi.toString()
    override fun toString(): String = asString()
}

data class Eier(val navident: String?, val navn: String?)

data class StillingsinfoDto(
    val stillingsid: String,
    val stillingsinfoid: String,
    val eierNavident: String?,
    val eierNavn: String?,
    val stillingskategori: Stillingskategori?
)

enum class Stillingskategori {
    STILLING, FORMIDLING, ARBEIDSTRENING, JOBBMESSE;

    companion object {
        fun fraDatabase(verdi: String?) = if (verdi == null) null else values().firstOrNull { it.name == verdi }
    }
}

