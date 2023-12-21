package no.nav.rekrutteringsbistand.api.indekser

import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.skjul_stilling.SkjulStillingRepository
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
        val authorizedPartyUtils: AuthorizedPartyUtils,
        val skjulStillingRepository: SkjulStillingRepository
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

        val stillingsIder = inboundDto.stillinger.map { Stillingsid(it.uuid)  }

        val a = stillingsIder.associateBy { hentSkjult(it) }



        val stillingsInfoDtoer = stillingsinfoService
           .hentForStillinger(stillingsIder)
           .map { it.asStillingsinfoDto() }


        return ResponseEntity.ok(listOf())




    }

    private fun hentSkjult(stillingsid: Stillingsid): Boolean  {
        val status = skjulStillingRepository.hentSkjulestatus(stillingsid)
        return status?.utførtMarkereForSkjuling != null
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
