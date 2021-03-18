package no.nav.rekrutteringsbistand.api.stillingsinfo.indekser

import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/indekser/stillingsinfo")
@ProtectedWithClaims(issuer = "azuread") // TODO
class StillingsinfoIndekserController(
        val stillingsinfoService: StillingsinfoService,
        val authorizedPartyUtils: AuthorizedPartyUtils
) {

    @PostMapping("/bulk")
    fun getStillingsInfoBulk(@RequestBody inboundDto: BulkStillingsinfoInboundDto): ResponseEntity<List<StillingsinfoDto>> {


        if (!authorizedPartyUtils.kallKommerFraStillingIndekser()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val stillingsIder = inboundDto.uuider.map { Stillingsid(it) }
        val stillingsinfo: List<StillingsinfoDto> = stillingsinfoService
                .hentForStillinger(stillingsIder)
                .map { it.asStillingsinfoDto() }

        return ResponseEntity.ok(stillingsinfo)
    }
}

data class BulkStillingsinfoInboundDto(
        val uuider: List<String>
)
