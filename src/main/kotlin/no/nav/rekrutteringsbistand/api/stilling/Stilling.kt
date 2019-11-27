package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoDto
import java.time.LocalDateTime
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class Stilling(
        val id: Long?,
        val uuid: String?,
        val created: LocalDateTime?,
        val createdBy: String?,
        val updated: LocalDateTime?,
        val updatedBy: String?,
        val title: String?,
        val status: String?,
        val privacy: String?,
        val source: String?,
        val medium: String?,
        val reference: String?,
        val published: LocalDateTime?,
        val expires: LocalDateTime?,
        val employer: Arbeidsgiver?,
        val administration: Metadata?,
        val location: Geografi?,
        val locationList: List<Geografi> = ArrayList(),
        val categoryList: List<Kategori> = ArrayList(),
        val properties: Map<String, String> = HashMap(),
        val rekruttering: StillingsinfoDto?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Metadata(
        val status: String?,
        val comments: String?,
        val reportee: String?,
        val remarks: List<String> = ArrayList(),
        val navIdent: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Arbeidsgiver(
        val name: String?,
        val orgnr: String?,
        val status: String?,
        val parentOrgnr: String?,
        val publicName: String?,
        val deactivated: LocalDateTime?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Geografi(
        val postalCode: String?,
        val county: String?,
        val municipal: String?,
        val municipalCode: String?,
        val city: String?,
        val country: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Kategori(
        val code: String?,
        val categoryType: String?,
        val name: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Page<T>(
        val content: List<T>,
        val totalPages: Int,
        val totalElements: Int
)
