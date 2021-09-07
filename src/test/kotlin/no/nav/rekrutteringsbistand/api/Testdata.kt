package no.nav.rekrutteringsbistand.api

import no.nav.pam.stilling.ext.avro.*
import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingMedStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.util.*

object Testdata {

    val enVeileder = InnloggetVeileder("Clark.Kent@nav.no", "Clark Kent", "C12345")
    val enAnnenVeileder = InnloggetVeileder("Lex.Luthor@nav.no", "Lex Luthor", "Y123123")

    val enStillingMedStillingsinfo = StillingMedStillingsinfo(
        rekruttering = null,
        id = 1000,
        uuid = UUID.randomUUID().toString(),
        created = LocalDateTime.now(),
        createdBy = "nss-admin",
        updated = LocalDateTime.now(),
        updatedBy = "nss-admin",
        title = "testnss",
        status = "ACTIVE",
        privacy = "SHOW_ALL",
        source = "DIR",
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
        activationOnPublishingDate = null,
        properties = hashMapOf("adtext" to "teksten")
    )

    val etNotat = "notatet"

    val enAnnenStillingMedStillingsinfo = enStillingMedStillingsinfo.copy(
        id = 1001,
        uuid = UUID.randomUUID().toString(),
        reference = UUID.randomUUID().toString(),
        source = "ASS"
    )

    val enTredjeStillingMedStillingsinfo = enStillingMedStillingsinfo.copy(
        id = 1002,
        uuid = UUID.randomUUID().toString(),
        reference = UUID.randomUUID().toString(),
        source = "ASS"
    )

    val enFjerdeStillingMedStillingsinfo = enStillingMedStillingsinfo.copy(
        id = 1003,
        uuid = UUID.randomUUID().toString(),
        reference = UUID.randomUUID().toString(),
        source = "ASS"
    )

    val enStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enStillingMedStillingsinfo.uuid!!),
        notat = etNotat
    )

    val enAnnenStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enAnnenStillingMedStillingsinfo.uuid!!),
        notat = etNotat
    )

    val enTredjeStillingsinfo = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
        stillingsid = Stillingsid(enTredjeStillingMedStillingsinfo.uuid!!),
        notat = etNotat
    )

    val enStillinggsinfoUtenEier = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        eier = null,
        stillingsid = Stillingsid(enFjerdeStillingMedStillingsinfo.uuid!!),
        notat = etNotat
    )

    val enStillingsinfoOppdatering = OppdaterEier(
        stillingsinfoid = enStillingsinfo.stillingsinfoid,
        eier = Eier(navident = enAnnenVeileder.navIdent, navn = enAnnenVeileder.displayName)
    )

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

    val enRekrutteringsbistandStilling = RekrutteringsbistandStilling(
        stillingsinfo = StillingsinfoDto(
            stillingsinfoid = enTredjeStillingsinfo.stillingsinfoid.asString(),
            eierNavident = enVeileder.navIdent,
            eierNavn = enVeileder.displayName,
            notat = etNotat,
            stillingsid = enStillingMedStillingsinfo.uuid!!
        ),
        stilling = enTredjeStillingMedStillingsinfo.tilStilling()

    )

    val enRekrutteringsbistandStillingUtenEier = RekrutteringsbistandStilling(
        stillingsinfo = StillingsinfoDto(
            stillingsinfoid = enStillinggsinfoUtenEier.stillingsinfoid.asString(),
            eierNavident = null,
            eierNavn = null,
            notat = "etAnnetNotat",
            stillingsid = enStillinggsinfoUtenEier.stillingsid.asString()
        ),
        stilling = enFjerdeStillingMedStillingsinfo.tilStilling()
    )

    val enPageMedStilling = Page(
        content = listOf(enStilling),
        totalElements = 1,
        totalPages = 1
    )

    fun enAd(stillingsId: String = UUID.randomUUID().toString(), tags: String) = Ad(
        stillingsId,
        "annonsenr",
        "tittel",
        AdStatus.ACTIVE,
        PrivacyChannel.INTERNAL_NOT_SHOWN,
        Administration(
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
        Administration(
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
