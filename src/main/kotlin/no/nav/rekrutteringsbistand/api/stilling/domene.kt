package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingAdministrationDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

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
    fun toKopiertStilling(tokenUtils: TokenUtils): OpprettStillingDto {
        return lagNyStilling(
            tittel = "Kopi - $title",
            tokenUtils
        ).copy(
            mediaList = mediaList,
            contactList = contactList,
            medium = medium,
            employer = employer,
            location = location,
            locationList = locationList,
            categoryList = categoryList,
            properties = properties,
            businessName = businessName,
            firstPublished = firstPublished,
            deactivatedByExpiry = deactivatedByExpiry,
            activationOnPublishingDate = activationOnPublishingDate,
        )
    }

}

fun lagNyStilling(tittel: String = "Ny stilling", tokenUtils: TokenUtils): OpprettStillingDto {
    return OpprettStillingDto(
        title = tittel,
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = OpprettStillingAdministrationDto(
            status = "PENDING",
            reportee = tokenUtils.hentInnloggetVeileder().displayName,
            navIdent = tokenUtils.hentInnloggetVeileder().navIdent,
        ),
    )
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
)

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
