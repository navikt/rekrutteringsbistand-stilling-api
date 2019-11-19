package no.nav.rekrutteringsbistand.api

import no.nav.rekrutteringsbistand.api.autorisasjon.InnloggetBruker
import no.nav.rekrutteringsbistand.api.stillingsinfo.*

object Testdata {
    val enVeileder = InnloggetBruker("Clark.Kent@nav.no", "Clark Kent", "C12345")

    val enStillingsinfo = Stillingsinfo(
            stillingsinfoid = Stillingsinfoid("12312312-51a9-4ca3-994d-41e3ab0e8204"),
            eier = Eier(navident = enVeileder.navIdent, navn = enVeileder.displayName),
            stillingsid = Stillingsid("ee82f29c-51a9-4ca3-994d-45e3ab0e8204"))

    val enStillingsinfoOppdatering = OppdaterStillingsinfo(
            stillingsinfoid = Stillingsinfoid("12312312-51a9-4ca3-994d-41e3ab0e8204"),
            eier = Eier(navident = "Y123123", navn = "Nytt navn"))
}
