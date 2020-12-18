package no.nav.rekrutteringsbistand.api.stillingsinfo.indekser

import no.nav.rekrutteringsbistand.api.option.None
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/indekser/stillingsinfo")
@ProtectedWithClaims(issuer = "azuread")
class StillingsinfoIndekserController(val stillingsinfoService: StillingsinfoService) {

    @GetMapping("/{stillingsId}")
    fun getStillingsInfo(@PathVariable stillingsId: Stillingsid): ResponseEntity<StillingsinfoDto> {
        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentForStilling(stillingsId)

        return when (stillingsinfo) {
            is None -> ResponseEntity.notFound().build()
            is Some -> ResponseEntity.ok(stillingsinfo.b.asStillingsinfoDto())
        }
    }
}
