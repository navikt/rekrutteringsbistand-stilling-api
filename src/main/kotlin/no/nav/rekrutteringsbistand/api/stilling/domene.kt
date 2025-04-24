package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingAdministrationDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.Kategori.Companion.hentTittel
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.rekrutteringsbistand.api.support.log
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Stilling(
    val id: Long,
    val uuid: String,
    val created: LocalDateTime,
    val createdBy: String,
    val updated: LocalDateTime,
    val updatedBy: String,
    val title: String,
    val status: String,

    val administration: Administration?,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val published: LocalDateTime?,
    val expires: LocalDateTime?,
    val employer: Arbeidsgiver?,
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val categoryList: List<Kategori> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val publishedByAdmin: String?,
    val businessName: String?,
    val firstPublished: Boolean?,
    val deactivatedByExpiry: Boolean?,
    val activationOnPublishingDate: Boolean?
) {
    companion object {
        const val DEFAULT_EXPIRY_DAYS: Long = 30
    }

    fun toKopiertStilling(tokenUtils: TokenUtils): no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto {
        val nyTittel = categoryList.hentTittel("kopi av stillingsId $uuid som ble opprettet $created")

        return no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto(
            tittel = nyTittel,
            tokenUtils
        ).copy(
            mediaList = mediaList,
            contactList = contactList,
            medium = medium,
            employer = employer,
            locationList = locationList,
            categoryList = categoryList,
            properties = properties,
            businessName = businessName,
            firstPublished = firstPublished,
            deactivatedByExpiry = deactivatedByExpiry,
            activationOnPublishingDate = activationOnPublishingDate,
        )
    }

    fun toDirektemeldtStillingInnhold(): DirektemeldtStillingInnhold {
        return DirektemeldtStillingInnhold(
            title = title,
            administration = administration?.let {
                DirektemeldtStillingAdministration(
                    status = it.status,
                    comments = it.comments,
                    reportee = it.reportee,
                    navIdent = it.navIdent,
                    remarks = it.remarks
                )
            },
            mediaList = mediaList,
            contactList = contactList,
            privacy = privacy,
            source = source,
            medium = medium,
            reference = reference,
            employer = employer?.toDirektemeldtStillingArbeidsgiver(),
            location = location,
            locationList = locationList,
            categoryList = categoryList.map { it.toDirektemeldtStillingKategori() },
            properties = properties,
            businessName = businessName,
            firstPublished = firstPublished,
            deactivatedByExpiry = deactivatedByExpiry,
            activationOnPublishingDate = activationOnPublishingDate,
        )
    }

    fun copyMedBeregnetTitle(stillingskategori: Stillingskategori?): Stilling =
        when (stillingskategori) {
            Stillingskategori.JOBBMESSE ->
                this.copy(title = "Invitasjon til jobbmesse")

            null,
            Stillingskategori.STILLING,
            Stillingskategori.FORMIDLING,
            Stillingskategori.ARBEIDSTRENING ->
                this.copy(title = hentInternEllerEksternTittel())
        }

    fun hentInternEllerEksternTittel(): String =
        if (erDirektemeldt())
            categoryList.hentTittel(kontekstForLoggmelding = "stillingsId $uuid opprettet $created")
        else
            title

    fun hentExpiresMedDefaultVerdiOmIkkeOppgitt(): ZonedDateTime {
        if (expires != null) {
            return expires.atZone(ZoneId.of("Europe/Oslo"))
        }

        // Sett expires lik søknadsfrist om den er oppgitt
        val applicationDue = properties["applicationdue"]
        if (!applicationDue.isNullOrBlank() && applicationDue.trim().uppercase() != "SNAREST") {
            try {
                return LocalDateTime.parse(applicationDue).atZone(ZoneId.of("Europe/Oslo"))
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Bruk default utløpsdato
        return LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS).atZone(ZoneId.of("Europe/Oslo"))
    }

    private fun erDirektemeldt(): Boolean = source == "DIR"
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class Administration(
    val id: Int?,
    val status: String?,
    val comments: String?,
    val reportee: String?,
    val remarks: List<String> = ArrayList(),
    val navIdent: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsgiver(
    val id: Int?,
    val uuid: String?,
    val created: String?,
    val createdBy: String?,
    val updated: String?,
    val updatedBy: String?,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val name: String?,
    val orgnr: String?,
    val status: String?,
    val parentOrgnr: String?,
    val publicName: String?,
    val deactivated: LocalDateTime?,
    val orgform: String?,
    val employees: Int?
) {
    fun toDirektemeldtStillingArbeidsgiver(): DirektemeldtStillingArbeidsgiver {
        return DirektemeldtStillingArbeidsgiver(
            mediaList = mediaList,
            contactList = contactList,
            location = location,
            locationList = locationList,
            properties = properties,
            name = name,
            orgnr = orgnr,
            parentOrgnr = parentOrgnr,
            publicName = publicName,
            orgform = orgform,
            employees = employees
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Geografi(
    val address: String?,
    val postalCode: String?,
    val county: String?,
    val municipal: String?,
    val municipalCode: String?,
    val city: String?,
    val country: String?,
    val latitude: String?,
    val longitude: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kategori(
    val id: Int?,
    val code: String?,
    val categoryType: String?,
    val name: String?,
    val description: String?,
    val parentId: Int?
) {
    fun toNonNullKategori() =
        if (code != null && name != null && categoryType != null)
            NonNullKategori(code = code, name = name, categoryType = categoryType)
        else
            null

    fun erJanzz() = categoryType == "JANZZ"
    fun erStyrk08Nav() = code?.matches(styrk08SeksSiffer) == true

    companion object {
        private val styrk08SeksSiffer = Regex("""^[0-9]{4}\.[0-9]{2}$""")

        fun List<Kategori>.hentTittel(kontekstForLoggmelding: String): String {
            if (filter(Kategori::erJanzz).size > 1) {
                log.warn("Mer enn én JANZZ-kategori funnet for kategori i $kontekstForLoggmelding. Velger en tilfeldig.")
            }
            if (filter(Kategori::erStyrk08Nav).size > 1) {
                log.warn("Mer enn én STYRK08Nav-kategori funnet for kategori i $kontekstForLoggmelding. Velger en tilfeldig.")
            }
            return find(Kategori::erStyrk08Nav)?.name
                ?: find(Kategori::erJanzz)?.name
                ?: "Stilling uten valgt jobbtittel"
        }
    }

    fun toDirektemeldtStillingKategori(): DirektemeldtStillingKategori {
        return DirektemeldtStillingKategori(
            code = code,
            categoryType = categoryType,
            name = name,
            description = description,
            parentId = parentId
        )
    }

}

data class NonNullKategori(
    val code: String,
    val categoryType: String,
    val name: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Contact(
    val name: String?,
    val email: String?,
    val phone: String?,
    val role: String?,
    val title: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Media(
    val mediaLink: String?,
    val filename: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Page<T>(
    val content: List<T> = ArrayList(),
    val totalPages: Int?,
    val totalElements: Int?
)

data class OpprettRekrutteringsbistandstillingDto(
    val stilling: OpprettStillingDto,
    val kategori: Stillingskategori
)


// TODO: denne klassen ble duplisert fra arbeidsplassen-domenet, og inneholder kanskje
//  properties som ikke blir sendt fra frontend, og som derfor kan slettes.
data class OpprettStillingDto(
    val createdBy: String,
    val updatedBy: String,
    val privacy: String?,
    val source: String?,
    val administration: OpprettStillingAdministrationDto,

    val mediaList: List<Media>? = ArrayList(),
    val contactList: List<Contact>? = ArrayList(),
    val medium: String? = null,
    val employer: Arbeidsgiver? = null,
    val location: Geografi? = null,
    val locationList: List<Geografi>? = ArrayList(),
    val categoryList: List<Kategori>? = ArrayList(),
    val properties: Map<String, String>? = HashMap(),
    val businessName: String? = null,
    val firstPublished: Boolean? = null,
    val deactivatedByExpiry: Boolean? = null,
    val activationOnPublishingDate: Boolean? = null
) {
    fun toArbeidsplassenDto(title: String) = no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto(
        title = title,
        createdBy = createdBy,
        updatedBy = updatedBy,
        privacy = privacy,
        source = source,
        administration = administration,
        mediaList = mediaList,
        contactList = contactList,
        medium = medium,
        employer = employer,
        locationList = locationList,
        categoryList = categoryList,
        properties = properties,
        businessName = businessName,
        firstPublished = firstPublished,
        deactivatedByExpiry = deactivatedByExpiry,
        activationOnPublishingDate = activationOnPublishingDate,
    )
}

data class DirektemeldtStillingKategori(
    val code: String?,
    val categoryType: String?,
    val name: String?,
    val description: String?,
    val parentId: Int?
) {
    fun toKategori(): Kategori {
        return Kategori(
            id = null,
            code = code,
            categoryType = categoryType,
            name = name,
            description = description,
            parentId = parentId
        )
    }
}

data class DirektemeldtStillingAdministration(
    val status: String?,
    val comments: String?,
    val reportee: String?,
    val remarks: List<String> = ArrayList(),
    val navIdent: String?
) {
    fun toAdministration(): Administration {
        return Administration(
            id = 0, // hvor kommer denne fra?
            status = status,
            comments = comments,
            reportee = reportee,
            remarks = remarks,
            navIdent = navIdent
        )
    }
}

data class DirektemeldtStillingArbeidsgiver(
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val name: String?,
    val orgnr: String?,
    val parentOrgnr: String?,
    val publicName: String?,
    val orgform: String?,
    val employees: Int?
) {
    fun toArbeidsgiver(): Arbeidsgiver {
        return Arbeidsgiver(
            id = null,
            uuid = null,
            created = null,
            createdBy = null,
            updated = null,
            updatedBy = null,
            mediaList = mediaList,
            contactList = contactList,
            location = location,
            locationList = locationList,
            properties = properties,
            name = name,
            orgnr = orgnr,
            status = null,
            parentOrgnr = parentOrgnr,
            publicName = publicName,
            deactivated = null,
            orgform = orgform,
            employees = employees
        )
    }
}

data class DirektemeldtStillingInnhold(
    val title: String,
    val administration: DirektemeldtStillingAdministration?,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val employer: DirektemeldtStillingArbeidsgiver?,
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val categoryList: List<DirektemeldtStillingKategori> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val businessName: String?,
    val firstPublished: Boolean?,
    val deactivatedByExpiry: Boolean?,
    val activationOnPublishingDate: Boolean?
)

data class DirektemeldtStilling(
    val stillingsId: UUID,
    val innhold: DirektemeldtStillingInnhold,
    val opprettet: ZonedDateTime,
    val opprettetAv: String,
    val sistEndret: ZonedDateTime,
    val sistEndretAv: String,
    val status: String,
    val annonseId: Long?,
    val versjon: Int = 1,
    val utløpsdato: ZonedDateTime? = null,
    val publisert: ZonedDateTime? = null,
    val publisertAvAdmin: String?,
    val adminStatus: String?
) {
    fun toStilling(): Stilling {
        return Stilling(
            id = annonseId ?: 0,
            uuid = stillingsId.toString(),
            created = opprettet.toLocalDateTime(),
            createdBy = opprettetAv,
            updated = sistEndret.toLocalDateTime(),
            updatedBy = sistEndretAv,
            title = innhold.title,
            status = status,
            administration = innhold.administration?.toAdministration(),
            mediaList = innhold.mediaList,
            contactList = innhold.contactList,
            privacy = innhold.privacy,
            source = innhold.source,
            medium = innhold.medium,
            reference = innhold.reference,
            published = publisert?.toLocalDateTime(),
            expires = utløpsdato?.toLocalDateTime(),
            employer = innhold.employer?.toArbeidsgiver(),
            location = innhold.location,
            locationList = innhold.locationList,
            categoryList = innhold.categoryList.map { it.toKategori() },
            properties = innhold.properties,
            publishedByAdmin = publisertAvAdmin,
            businessName = innhold.businessName,
            firstPublished = innhold.firstPublished,
            deactivatedByExpiry = innhold.deactivatedByExpiry,
            activationOnPublishingDate = innhold.activationOnPublishingDate
        )
    }
}

enum class Status {
    ACTIVE,
    INACTIVE,
    DONE,
    DELETED,
    STOPPED,
    REJECTED;

    override fun toString(): String {
        return name
    }
}

