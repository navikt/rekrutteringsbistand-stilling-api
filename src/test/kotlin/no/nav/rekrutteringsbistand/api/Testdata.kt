package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetBruker
import no.nav.rekrutteringsbistand.api.rekrutteringsbistand.OppdaterRekrutteringsbistand
import no.nav.rekrutteringsbistand.api.rekrutteringsbistand.Rekrutteringsbistand

object Testdata {
    val enVeileder = InnloggetBruker("Clark.Kent@nav.no", "Clark Kent", "C12345")

    val etRekrutteringsbistand = Rekrutteringsbistand(
            rekrutteringUuid = "12312312-51a9-4ca3-994d-41e3ab0e8204",
            eierIdent = enVeileder.navIdent,
            eierNavn = enVeileder.displayName,
            stillingUuid = "ee82f29c-51a9-4ca3-994d-45e3ab0e8204")

    val enRekrutteringsbistandOppdatering = OppdaterRekrutteringsbistand(
            rekrutteringsUuid = "12312312-51a9-4ca3-994d-41e3ab0e8204",
            eierIdent = "Y123123",
            eierNavn = "Nytt navn")
}
