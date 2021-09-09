package no.nav.rekrutteringsbistand.api

import no.nav.pam.stilling.ext.avro.*
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.stilling.*
import no.nav.rekrutteringsbistand.api.stilling.Administration
import no.nav.rekrutteringsbistand.api.stilling.ekstern.StillingForPersonbruker
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.util.*

object Testdata {

    val enVeileder = InnloggetVeileder("Clark.Kent@nav.no", "Clark Kent", "C12345")

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
        createdBy= "pam-rekrutteringsbistand",
        updatedBy= "pam-rekrutteringsbistand",
        source= "DIR",
        privacy= "INTERNAL_NOT_SHOWN",
        administration= OpprettStillingAdministrationDto(
            status= "PENDING",
            reportee = enVeileder.displayName,
            navIdent = enVeileder.navIdent,
        ),
    )

    val enOpprettetStilling = Stilling(
        title = "Ny stilling",
        createdBy= "pam-rekrutteringsbistand",
        updatedBy= "pam-rekrutteringsbistand",
        source= "DIR",
        privacy= "INTERNAL_NOT_SHOWN",
        administration= Administration(
            status= "PENDING",
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
        businessName = null,
        deactivatedByExpiry = null,
        firstPublished = null,
        publishedByAdmin = null,
        properties = emptyMap(),
        location = null,
        status = null,
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
        stillingsid = Stillingsid(enStilling.uuid!!),
        notat = etNotat
    )

    val enStillingsinfoUtenEier = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = null,
        stillingsid = Stillingsid(enStilling.uuid!!),
        notat = etNotat
    )

    val enAnnenStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enAnnenStilling.uuid!!),
        notat = etNotat
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

    fun enAd(stillingsId: String = UUID.randomUUID().toString(), tags: String) = Ad(
        stillingsId,
        "annonsenr",
        "tittel",
        AdStatus.ACTIVE,
        PrivacyChannel.INTERNAL_NOT_SHOWN,
        no.nav.pam.stilling.ext.avro.Administration(
            AdministrationStatus.DONE,
            listOf(RemarkType.FOREIGN_JOB),
            "kommentar",
            "reportee",
            "navIdent"
        ),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        Company(
            "navn",
            "publicname",
            "orgnr",
            "parentOrgnr",
            "orgform"
        ),
        listOf(StyrkCategory("kode", "name")),
        "source",
        "medium",
        "reference",
        LocalDateTime.now().toString(),
        "businessName",
        listOf(
            Location(
                "address",
                "postalCode",
                "county",
                "municipal",
                "country",
                "latitue",
                "longitude",
                "municipal_code",
                "county_code"
            )
        ),
        listOf(
            Property("sector", "Offentlig"),
            Property("adtext", "<h1>Tittel</h2><p>Den beste stillingen <b>noen sinne</b></p>"),
            Property(
                "searchtags",
                "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"
            ),
            Property("tags", tags)
        )
    )

    fun enAdUtenTag(stillingsId: String = UUID.randomUUID().toString()) = Ad(
        stillingsId,
        "annonsenr",
        "tittel",
        AdStatus.ACTIVE,
        PrivacyChannel.INTERNAL_NOT_SHOWN,
        no.nav.pam.stilling.ext.avro.Administration(
            AdministrationStatus.DONE,
            listOf(RemarkType.FOREIGN_JOB),
            "kommentar",
            "reportee",
            "navIdent"
        ),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        LocalDateTime.now().toString(),
        Company(
            "navn",
            "publicname",
            "orgnr",
            "parentOrgnr",
            "orgform"
        ),
        listOf(StyrkCategory("kode", "name")),
        "source",
        "medium",
        "reference",
        LocalDateTime.now().toString(),
        "businessName",
        listOf(
            Location(
                "address",
                "postalCode",
                "county",
                "municipal",
                "country",
                "latitue",
                "longitude",
                "municipal_code",
                "county_code"
            )
        ),
        listOf(
            Property("sector", "Offentlig"),
            Property("adtext", "<h1>Tittel</h2><p>Den beste stillingen <b>noen sinne</b></p>"),
            Property(
                "searchtags",
                "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"
            ),
        )
    )

}
