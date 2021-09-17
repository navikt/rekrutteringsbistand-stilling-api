package no.nav.rekrutteringsbistand.api.stillingsinfo

import java.sql.ResultSet
import java.util.*

data class Stillingsinfo(
    val stillingsinfoid: Stillingsinfoid,
    val stillingsid: Stillingsid,
    val eier: Eier?,
    val notat: String?,
    val oppdragKategori: OppdragKategori?
) {
    fun asStillingsinfoDto() = StillingsinfoDto(
        stillingsid = this.stillingsid.asString(),
        stillingsinfoid = this.stillingsinfoid.toString(),
        notat = this.notat,
        eierNavident = this.eier?.navident,
        eierNavn = this.eier?.navn,
        oppdragKategori = this.oppdragKategori
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
                notat = rs.getString("notat"),
                oppdragKategori = OppdragKategori.fraDatabaseString(rs.getString("oppdrag_kategori"))
            )
    }
}

data class OppdaterEier(val stillingsinfoid: Stillingsinfoid, val eier: Eier)

data class OppdaterNotat(val stillingsinfoid: Stillingsinfoid, val notat: String)

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
    val eierNavn: String?,
    val oppdragKategori: OppdragKategori?
)

enum class OppdragKategori(private val verdi: String){
    Stilling("Stilling"),
    Arrangement("Arrangement"),
    Webinar("Webinar"),
    Formidling("Formidling"),
    Arbeidstrening("Arbeidstrening"),
    Annet("Annet");

    companion object {
        fun fraDatabaseString(value: String) = OppdragKategori.valueOf(value)
    }
}

