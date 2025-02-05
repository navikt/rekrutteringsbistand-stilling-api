package no.nav.rekrutteringsbistand.api.arbeidsplassen

import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.*
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
}


data class OpprettStillingAdministrationDto(
    val status: String,
    val reportee: String,
    val navIdent: String,
) {
    fun toDirekteMeldtStillingAdministration() = DirektemeldtStillingAdministration(
        status = status,
        comments = null,
        reportee = reportee,
        remarks = emptyList(),
        navIdent = navIdent,
    )
}
