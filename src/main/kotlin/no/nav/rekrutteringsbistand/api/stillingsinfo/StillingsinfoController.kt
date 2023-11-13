package no.nav.rekrutteringsbistand.api.stillingsinfo

import no.nav.rekrutteringsbistand.api.support.secureLog
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
        val stillingsid = Stillingsid(dto.stillingsid)

        val forrigeStillingsinfo = service.hentForStilling(stillingId = stillingsid)
        val forrigeEier = forrigeStillingsinfo.orNull()?.eier?.navident

        secureLog.info("Overtar ekstern stilling og kandidatliste gammel eier $forrigeEier ny eier ${dto.eierNavident} stillingsid ${dto.stillingsid} (under utvikling)")
        val nyEier = Eier(dto.eierNavident, dto.eierNavn)
        val oppdatertStillingsinfo =
            service.overtaEierskapForEksternStillingOgKandidatliste(stillingsId = stillingsid, nyEier = nyEier)

        return ResponseEntity.status(HttpStatus.OK).body(oppdatertStillingsinfo.asStillingsinfoDto())
    }
}

data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavident: String,
    val eierNavn: String
)
