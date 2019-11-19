package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetBruker
import no.nav.rekrutteringsbistand.api.rekrutteringsbistand.*

object Testdata {
    val enVeileder = InnloggetBruker("Clark.Kent@nav.no", "Clark Kent", "C12345")

    val etRekrutteringsbistand = Rekrutteringsbistand(
            rekrutteringId = RekrutteringId("12312312-51a9-4ca3-994d-41e3ab0e8204"),
            eier = Eier(ident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingId = StillingId("ee82f29c-51a9-4ca3-994d-45e3ab0e8204"))

    val enRekrutteringsbistandOppdatering = OppdaterRekrutteringsbistand(
            rekrutteringsUuid = "12312312-51a9-4ca3-994d-41e3ab0e8204",
            eierIdent = "Y123123",
            eierNavn = "Nytt navn")
}
