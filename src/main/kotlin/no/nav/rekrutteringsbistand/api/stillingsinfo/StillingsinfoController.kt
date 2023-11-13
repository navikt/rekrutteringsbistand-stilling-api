package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.support.log
import no.nav.rekrutteringsbistand.api.support.secureLog
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/stillingsinfo")
@Protected
class StillingsinfoController(
    val service: StillingsinfoService,
) {
    @PutMapping
    fun overtaEierskapForEksternStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<StillingsinfoDto> {
        secureLog.info("Overtar ekstern stilling og kandidatliste ident ${dto.eierNavident} stillingsid ${dto.stillingsid}")
        val nyEier = Eier(dto.eierNavident, dto.eierNavn)
        val oppdatertStillingsinfo = service.overtaEierskapForEksternStillingOgKandidatliste(Stillingsid(dto.stillingsid), nyEier)

        return ResponseEntity.status(HttpStatus.OK).body(oppdatertStillingsinfo.asStillingsinfoDto())
    }
}

data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavident: String,
    val eierNavn: String
)
