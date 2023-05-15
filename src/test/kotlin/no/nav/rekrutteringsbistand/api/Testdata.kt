package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingAdministrationDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.stilling.Administration
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.ekstern.StillingForPersonbruker
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.util.*

object Testdata {

    val enVeileder = InnloggetVeileder("Clark Kent", "C12345")

    const val etNotat = "notatet"

    val enStilling = Stilling(
        id = 1000,
        uuid = UUID.randomUUID().toString(),
        created = LocalDateTime.now(),
        createdBy = "nss-admin",
        updated = LocalDateTime.now(),
        updatedBy = "nss-admin",
        title = "testnss",
        status = "ACTIVE",
        privacy = "SHOW_ALL",
        source = "ASS",
        medium = "ASS",
        reference = UUID.randomUUID().toString(),
        published = LocalDateTime.now(),
        expires = LocalDateTime.now(),
        employer = null,
        administration = null,
        location = null,
        publishedByAdmin = null,
        businessName = null,
        firstPublished = null,
        deactivatedByExpiry = null,
        activationOnPublishingDate = null
    )

    val enOpprettStillingDto = OpprettStillingDto(
        title = "Ny stilling",
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = OpprettStillingAdministrationDto(
            status = "PENDING",
            reportee = enVeileder.displayName,
            navIdent = enVeileder.navIdent,
        ),
        businessName = "En Ansvarlig Arbeidsgiver AS"
    )

    val enOpprettRekrutteringsbistandstillingDto = OpprettRekrutteringsbistandstillingDto(
        stilling = enOpprettStillingDto,
        kategori = Stillingskategori.ARBEIDSTRENING
    )

    val enOpprettetStilling = Stilling(
        title = "Ny stilling",
        createdBy = "pam-rekrutteringsbistand",
        updatedBy = "pam-rekrutteringsbistand",
        source = "DIR",
        privacy = "INTERNAL_NOT_SHOWN",
        administration = Administration(
            status = "PENDING",
            reportee = enVeileder.displayName,
            navIdent = enVeileder.navIdent,
            id = 0,
            comments = "",
            remarks = emptyList()
        ),
        id = 1000,
        uuid = UUID.randomUUID().toString(),
        created = LocalDateTime.now(),
        updated = LocalDateTime.now(),
        reference = UUID.randomUUID().toString(),
        published = LocalDateTime.now(),
        expires = LocalDateTime.now(),
        activationOnPublishingDate = null,
        categoryList = emptyList(),
        mediaList = emptyList(),
        locationList = emptyList(),
        contactList = emptyList(),
        businessName = "En Ansvarlig Arbeidsgiver AS",
        deactivatedByExpiry = null,
        firstPublished = null,
        publishedByAdmin = null,
        properties = emptyMap(),
        location = null,
        status = "ACTIVE",
        employer = null,
        medium = null,
    )

    val enAnnenStilling = enStilling.copy(
        id = 1001,
        uuid = UUID.randomUUID().toString(),
        title = "En annen stilling"
    )

    val enPageMedStilling = Page(
        content = listOf(enStilling),
        totalElements = 1,
        totalPages = 1
    )

    val enStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enStilling.uuid),
        notat = etNotat,
        stillingskategori = null
    )

    val enStillingsinfoInboundDto = StillingsinfoInboundDto(
        stillingsid = enStilling.uuid,
        eierNavident = enVeileder.navIdent,
        eierNavn = enVeileder.displayName
    )

    val enStillingsinfoUtenEier = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = null,
        stillingsid = Stillingsid(enStilling.uuid),
        notat = etNotat,
        stillingskategori = null
    )

    val enAnnenStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enAnnenStilling.uuid),
        notat = etNotat,
        stillingskategori = null
    )

    val enStillingsinfoOppdatering = OppdaterEier(
        stillingsinfoid = enStillingsinfo.stillingsinfoid,
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName)
    )

    val enRekrutteringsbistandStilling = RekrutteringsbistandStilling(
        stillingsinfo = enStillingsinfo.asStillingsinfoDto(),
        stilling = enStilling
    )

    val enRekrutteringsbistandStillingUtenEier = RekrutteringsbistandStilling(
        stillingsinfo = enStillingsinfoUtenEier.asStillingsinfoDto(),
        stilling = enStilling
    )

    val enStillingForPersonbruker = StillingForPersonbruker(
        id = enStilling.id,
        updated = enStilling.updated,
        title = enStilling.title,
        medium = enStilling.medium,
        employer = null,
        location = enStilling.location,
        properties = enStilling.properties,
        businessName = enStilling.businessName,
        status = enStilling.status,
        uuid = enStilling.uuid,
        source = enStilling.source
    )
}
