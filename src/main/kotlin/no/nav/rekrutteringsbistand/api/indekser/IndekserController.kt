package no.nav.rekrutteringsbistand.api.indekser

import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/indekser")
@ProtectedWithClaims(issuer = "azuread")
class IndekserController(
        val stillingsinfoService: StillingsinfoService,
        val authorizedPartyUtils: AuthorizedPartyUtils
) {

    @PostMapping("/stillingsinfo/bulk")
    fun getStillingsInfoBulk(@RequestBody inboundDto: BulkStillingsinfoInboundDto): ResponseEntity<List<StillingsinfoDto>> {
        if (!authorizedPartyUtils.kallKommerFraStillingIndekser()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val stillingsIder = inboundDto.uuider.map { Stillingsid(it) }
        val stillingsinfo: List<StillingsinfoDto> = stillingsinfoService
                .hentForStillinger(stillingsIder)
                .map { it.asStillingsinfoDto() }

        return ResponseEntity.ok(stillingsinfo)
    }

    @PostMapping("/berik_stillinger")
    fun berikStillinger(@RequestBody inboundDto: BerikStillingerRequestBodyDto): ResponseEntity<List<BerikStillingerResponseDto>> {
        if (!authorizedPartyUtils.kallKommerFraStillingIndekser()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

//        val stillingsIder = inboundDto.uuider.map { Stillingsid(it) }
//        val stillingsinfo: List<StillingsinfoDto> = stillingsinfoService
//            .hentForStillinger(stillingsIder)
//            .map { it.asStillingsinfoDto() }
//
//

        return ResponseEntity.ok(listOf())
    }
}

data class BulkStillingsinfoInboundDto(
        val uuider: List<String>
)

data class BerikStillingerRequestBodyDto(
    val stillinger: List<BerikStillingDto>,
)

data class BerikStillingDto(
    val uuid: String,
    val expires: LocalDate, /* tolkes i Europe/Oslo */
)

data class BerikStillingerResponseDto(
    val stillingsinfo: StillingsinfoDto?,
    val erSkjult: Boolean,
)
