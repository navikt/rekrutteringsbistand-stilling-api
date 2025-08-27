package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import java.time.LocalDateTime
import java.util.HashMap

data class ArbeidsplassenStillingDto(
    val id: Long,
    val uuid: String,
    val created: LocalDateTime,
    val createdBy: String,
    val updated: LocalDateTime,
    val updatedBy: String,
    val title: String,
    val status: String,

    val administration: Administration?,
    val mediaList: List<Media> = java.util.ArrayList(),
    val contactList: List<Contact> = java.util.ArrayList(),
    val privacy: String?,
    val source: String?,
    val medium: String?,
    val reference: String?,
    val published: LocalDateTime?,
    val expires: LocalDateTime?,
    val employer: Arbeidsgiver?,
    val location: Geografi?,
    val locationList: List<Geografi> = java.util.ArrayList(),
    val categoryList: List<Kategori> = java.util.ArrayList(),
    val properties: Map<String, String> = HashMap(),
    val publishedByAdmin: String?,
    val businessName: String?,
    val firstPublished: Boolean?,
    val deactivatedByExpiry: Boolean?,
    val activationOnPublishingDate: Boolean?
) {
    fun toStilling() = FrontendStilling(
        id = id,
        uuid = uuid,
        annonsenr = "",
        created = created,
        createdBy = createdBy,
        updated = updated,
        updatedBy = updatedBy,
        title = title,
        status = status,
        administration = administration,
        mediaList = mediaList,
        contactList = contactList,
        privacy = privacy,
        source = source,
        medium = medium,
        reference = reference,
        published = published,
        expires = expires,
        employer = employer,
        location = location,
        locationList = locationList,
        categoryList = categoryList,
        properties = properties,
        publishedByAdmin = publishedByAdmin,
        businessName = businessName,
        firstPublished = firstPublished,
        deactivatedByExpiry = deactivatedByExpiry,
        activationOnPublishingDate = activationOnPublishingDate,
        versjon = null
    )
}

data class OpprettStillingDto(
    val title: String,
    val createdBy: String,
    val updatedBy: String,
    val privacy: String?,
    val source: String?,
    val administration: OpprettStillingAdministrationDto,

    val mediaList: List<Media>? = ArrayList(),
    val contactList: List<Contact>? = ArrayList(),
    val medium: String? = "DIR",
    val employer: Arbeidsgiver? = null,
    val locationList: List<Geografi>? = ArrayList(),
    val categoryList: List<Kategori>? = ArrayList(),
    val properties: Map<String, String>? = HashMap(),
    val businessName: String? = null,
    val firstPublished: Boolean? = null,
    val deactivatedByExpiry: Boolean? = null,
    val activationOnPublishingDate: Boolean? = null
) {
    constructor(tittel: String = "Ny stilling", tokenUtils: TokenUtils): this(
        title = tittel,
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        medium = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = OpprettStillingAdministrationDto(
            status = "PENDING",
            reportee = tokenUtils.hentInnloggetVeileder().displayName,
            navIdent = tokenUtils.hentInnloggetVeileder().navIdent,
        ),
    )

     fun toDirektemeldtStillingInnhold(stillingsId: Stillingsid): DirektemeldtStillingInnhold {
        return DirektemeldtStillingInnhold(
            title = title,
            administration = administration.let {
                DirektemeldtStillingAdministration(
                    comments = null,
                    reportee = it.reportee,
                    navIdent = it.navIdent,
                    remarks = emptyList()
                )
            },
            mediaList = mediaList?: emptyList(),
            contactList = contactList ?: emptyList(),
            privacy = privacy,
            source = source,
            medium = medium,
            reference = stillingsId.asString(),
            employer = employer?.toDirektemeldtStillingArbeidsgiver(),
            location = null,
            locationList = locationList ?: emptyList(),
            categoryList = categoryList?.map { it.toDirektemeldtStillingKategori() } ?: emptyList(),
            properties = properties ?: emptyMap(),
            businessName = businessName,
            firstPublished = firstPublished,
            deactivatedByExpiry = deactivatedByExpiry,
            activationOnPublishingDate = activationOnPublishingDate,
        )
    }
}

data class OpprettStillingAdministrationDto(
    val status: String,
    val reportee: String,
    val navIdent: String,
)
