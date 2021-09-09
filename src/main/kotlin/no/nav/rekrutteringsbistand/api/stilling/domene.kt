package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

@JsonIgnoreProperties(ignoreUnknown = true)
data class Stilling(
    val id: Long?,
    val uuid: String?,
    val created: LocalDateTime?,
    val createdBy: String?,
    val updated: LocalDateTime?,
    val updatedBy: String?,
    val mediaList: List<Media> = ArrayList(),
    val contactList: List<Contact> = ArrayList(),
    val title: String?,
    val status: String?,
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val published: LocalDateTime?,
    val expires: LocalDateTime?,
    val employer: Arbeidsgiver?,
    val administration: Administration?,
    val location: Geografi?,
    val locationList: List<Geografi> = ArrayList(),
    val categoryList: List<Kategori> = ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val publishedByAdmin: String?,
    val businessName: String?,
    val firstPublished: Boolean?,
    val deactivatedByExpiry: Boolean?,
    val activationOnPublishingDate: Boolean?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettStillingDto(
    val title: String,
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
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Administration(
    val id: Int?,
    val status: String?,
    val comments: String?,
    val reportee: String?,
    val remarks: List<String> = ArrayList(),
    val navIdent: String?
)

data class OpprettStillingAdministrationDto(
    val status: String,
    val reportee: String,
    val navIdent: String,
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
