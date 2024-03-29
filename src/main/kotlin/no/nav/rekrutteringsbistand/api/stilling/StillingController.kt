package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest


@RestController
@Protected
class StillingController(val stillingService: StillingService) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        val opprettetStilling = stillingService.opprettNyStilling(stilling)
        return ok(opprettetStilling)
    }

    @PostMapping("/rekrutteringsbistandstilling/kopier/{stillingsId}")
    fun kopierStilling(@PathVariable stillingsId: String): ResponseEntity<RekrutteringsbistandStilling> {
        val kopiertStilling = stillingService.kopierStilling(stillingsId)
        return ok(kopiertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun oppdaterStilling(request: HttpServletRequest, @RequestBody rekrutteringsbistandStillingDto: OppdaterRekrutteringsbistandStillingDto): ResponseEntity<OppdaterRekrutteringsbistandStillingDto> {
        val oppdatertStilling = stillingService.oppdaterRekrutteringsbistandStilling(rekrutteringsbistandStillingDto, request.queryString)
        return ok(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistandstilling/{stillingsId}")
    fun slettRekrutteringsbistandStilling(@PathVariable(value = "stillingsId") stillingsId: String): ResponseEntity<Stilling> {
        val slettetStilling = stillingService.slettRekrutteringsbistandStilling(stillingsId)
        return ok(slettetStilling)
    }

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: String): ResponseEntity<RekrutteringsbistandStilling> {
        // TODO styrk(kan tas til slutt): Interne stillinger burde ikke sende tittel.
        return ok(stillingService.hentRekrutteringsbistandStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistandstilling/annonsenr/{annonsenr}")
    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(@PathVariable annonsenr: String): ResponseEntity<RekrutteringsbistandStilling> {
        // TODO styrk(kan tas til slutt): Interne stillinger burde ikke sende tittel.
        val stilling = stillingService.hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr)
        return stilling.map { ok(it) }.getOrElse { notFound().build() }
    }
}
