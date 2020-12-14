package no.nav.rekrutteringsbistand.api.stillingsinfo.indekser

import arrow.core.Either
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
        val stillingsinfo: Either<Unit, Stillingsinfo> = stillingsinfoService.hentForStilling(stillingsId)

        return when (stillingsinfo) {
            is Either.Left -> ResponseEntity.notFound().build()
            is Either.Right -> ResponseEntity.ok(stillingsinfo.b.asStillingsinfoDto())
        }
    }
}
