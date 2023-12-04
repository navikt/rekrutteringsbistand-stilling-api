package no.nav.rekrutteringsbistand.api.arbeidsplassen

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import java.util.HashMap

data class OpprettRekrutteringsbistandstillingDto(
    val stilling: OpprettStillingDto,
    val kategori: Stillingskategori
)

data class OpprettStillingDto(
    @JsonIgnoreProperties
    val title: String?,
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

data class OpprettStillingAdministrationDto(
    val status: String,
    val reportee: String,
    val navIdent: String,
)
