package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetBruker
import no.nav.rekrutteringsbistand.api.stilling.Stilling
import no.nav.rekrutteringsbistand.api.stilling.StillingMedStillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import java.time.LocalDateTime
import java.util.*

object Testdata {
    val enVeileder = InnloggetBruker("Clark.Kent@nav.no", "Clark Kent", "C12345")
    val enAnnenVeileder = InnloggetBruker("Lex.Luthor@nav.no", "Lex Luthor", "Y123123")

    val enStilling = StillingMedStillingsinfo(
            rekruttering = null,
            id = 1000,
            uuid = UUID.randomUUID().toString(),
            created = LocalDateTime.now(),
            createdBy = "nss-admin",
            updated = LocalDateTime.now(),
            updatedBy = "nss-admin",
            title = "testnss Æøå",
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

    val enAnnenStilling = enStilling.copy(
            id = 1001,
            uuid = UUID.randomUUID().toString(),
            reference = UUID.randomUUID().toString()
    )

    val enStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enStilling.uuid!!)
    )

    val enAnnenStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid(enAnnenStilling.uuid!!)
    )

    val enStillingsinfoOppdatering = OppdaterStillingsinfo(
            stillingsinfoid = enStillingsinfo.stillingsinfoid,
            eier = Eier(navident = enAnnenVeileder.navIdent, navn = enAnnenVeileder.displayName)
    )
}
