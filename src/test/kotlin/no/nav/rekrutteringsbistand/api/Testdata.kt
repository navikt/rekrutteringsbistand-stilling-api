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

    val enStilling = StillingMedStillingsinfo(
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

    val enAnnenStilling = enStilling.copy(
            id = 1001,
            uuid = UUID.randomUUID().toString(),
            reference = UUID.randomUUID().toString(),
            source = "ASS"
    )

    val enTredjeStilling = enStilling.copy(
            id = 1002,
            uuid = UUID.randomUUID().toString(),
            reference = UUID.randomUUID().toString(),
            source = "ASS"
    )

    val enFjerdeStilling = enStilling.copy(
            id = 1003,
            uuid = UUID.randomUUID().toString(),
            reference = UUID.randomUUID().toString(),
            source = "ASS"
    )

    val enStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enStilling.uuid!!),
            notat = etNotat
    )

    val enAnnenStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enAnnenStilling.uuid!!),
            notat = etNotat
    )

    val enTredjeStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enTredjeStilling.uuid!!),
            notat = etNotat
    )

    val enStillinggsinfoUtenEier = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = null,
            stillingsid = Stillingsid(enFjerdeStilling.uuid!!),
            notat = etNotat
    )

    val enStillingsinfoOppdatering = OppdaterEier(
            stillingsinfoid = enStillingsinfo.stillingsinfoid,
            eier = Eier(navident = enAnnenVeileder.navIdent, navn = enAnnenVeileder.displayName)
    )

    val enPage = Page(
            content = listOf(enStilling, enAnnenStilling),
            totalElements = 2,
            totalPages = 1
    )

    val enStillingUtenStillingsinfo = Stilling(
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

    val enRekrutteringsbistandStilling = HentRekrutteringsbistandStillingDto(
            stillingsinfo = StillingsinfoDto(
                    stillingsinfoid = enTredjeStillingsinfo.stillingsinfoid.asString(),
                    eierNavident = enVeileder.navIdent,
                    eierNavn = enVeileder.displayName,
                    notat = etNotat,
                    stillingsid = enStilling.uuid!!
            ),
            stilling = enTredjeStilling.tilStilling()

    )

    val enRekrutteringsbistandStillingUtenEier = HentRekrutteringsbistandStillingDto(
            stillingsinfo = StillingsinfoDto(
                    stillingsinfoid = enStillinggsinfoUtenEier.stillingsinfoid.asString(),
                    eierNavident = null,
                    eierNavn = null,
                    notat = "etAnnetNotat",
                    stillingsid = enStillinggsinfoUtenEier.stillingsid.asString()),
            stilling = enFjerdeStilling.tilStilling()
    )

    fun enAd(stillingsId: UUID = UUID.randomUUID(), tags: String) = Ad(
            stillingsId.toString(),
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
                    Property("searchtags", "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"),
                    Property("tags", tags)
            )
    )

    fun enAdUtenTag(stillingsId: UUID = UUID.randomUUID()) = Ad(
            stillingsId.toString(),
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
                    Property("searchtags", "[{\"label\":\"Sales Promotion Manager\",\"score\":1.0},{\"label\":\"Salgssjef\",\"score\":0.25137392},{\"label\":\"Sales Manager (Hotels)\",\"score\":0.21487874},{\"label\":\"Promotions Director\",\"score\":0.09032349},{\"label\":\"Salgsfremmer\",\"score\":0.09004237}]"),
            )
    )
}
