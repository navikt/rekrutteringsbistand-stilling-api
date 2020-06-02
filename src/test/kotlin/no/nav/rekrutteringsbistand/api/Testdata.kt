package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetVeileder
import no.nav.rekrutteringsbistand.api.stilling.Page
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingMedStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
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
                    eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
                    notat = etNotat,
                    stillingsid = enStilling.uuid!!
            ),
            stilling = enTredjeStilling.tilStilling()

            )

    val enRekrutteringsbistandStillingUtenEier = HentRekrutteringsbistandStillingDto(
            stillingsinfo = StillingsinfoDto(
                    stillingsinfoid = enStillinggsinfoUtenEier.stillingsinfoid.asString(),
                    eier = null,
                    notat = "etAnnetNotat",
                    stillingsid = enStillinggsinfoUtenEier.stillingsid.asString()),
            stilling = enFjerdeStilling.tilStilling()
    )

    val anyJsonRequestEntity: HttpEntity<String> by lazy {
        val requestHeaders = mapOf(
                HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
                HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
                HttpHeaders.AUTHORIZATION to "Bearer .*}").toMultiValueMap()
        val dummyRequestBody = "{}"
        HttpEntity(dummyRequestBody, requestHeaders)
    }
}
