package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import java.time.ZonedDateTime
import java.util.HashMap

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
        privacy = "INTERNAL_NOT_SHOWN",
        administration = OpprettStillingAdministrationDto(
            status = "PENDING",
            reportee = tokenUtils.hentInnloggetVeileder().displayName,
            navIdent = tokenUtils.hentInnloggetVeileder().navIdent,
        ),
    )

     fun toDirektemeldtStillingInnhold(stillingsId: Stillingsid, opprettet: ZonedDateTime): DirektemeldtStillingInnhold {
        return DirektemeldtStillingInnhold(
            title = title,
            administration = administration.let {
                DirektemeldtStillingAdministration(
                    status = it.status,
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
            published = opprettet,
            expires = null,
            employer = employer?.toDirektemeldtStillingArbeidsgiver(),
            location = null,
            locationList = locationList ?: emptyList(),
            categoryList = categoryList?.map { it.toDirektemeldtStillingKategori() } ?: emptyList(),
            properties = properties ?: emptyMap(),
            publishedByAdmin = null, 
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
