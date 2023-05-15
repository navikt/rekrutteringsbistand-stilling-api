package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.minestillinger.MineStillingerService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/stillingsinfo")
@Protected
class StillingsinfoController(
    private val service: StillingsinfoService,
    private val mineStillingerService: MineStillingerService,
) {
    @PutMapping
    fun overtaEierskapForEksternStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<StillingsinfoDto> {
        val nyEier = Eier(dto.eierNavident, dto.eierNavn)
        val rekrutteringsbistandStilling: RekrutteringsbistandStilling = service.overtaEierskapForEksternStillingOgKandidatliste(Stillingsid(dto.stillingsid), nyEier)
        mineStillingerService.overtaEierskap(rekrutteringsbistandStilling.stilling, dto.eierNavident)
        return ResponseEntity.status(HttpStatus.OK).body(rekrutteringsbistandStilling.stillingsinfo)
    }

    @GetMapping("/ident/{navident}")
    fun hentForIdent(@PathVariable navident: String): Collection<StillingsinfoDto> =
        service.hentForIdent(navident).map { it.asStillingsinfoDto() }
}

data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavident: String,
    val eierNavn: String
)
