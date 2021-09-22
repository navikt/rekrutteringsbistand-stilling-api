package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.servlet.http.HttpServletRequest


@RestController
@ProtectedWithClaims(issuer = "isso")
class StillingController(val stillingService: StillingService) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        val opprettetStilling = stillingService.opprettStilling(stilling)
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

    @DeleteMapping("/rekrutteringsbistand/api/v1/ads/{stillingsId}")
    fun slettStilling(@PathVariable(value = "stillingsId") stillingsId: String): ResponseEntity<Stilling> {
        val slettetStilling = stillingService.slettStilling(stillingsId)
        return ok(slettetStilling)
    }

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: String): ResponseEntity<RekrutteringsbistandStilling> {
        return ok(stillingService.hentRekrutteringsbistandStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistandstilling/annonsenr/{annonsenr}")
    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(@PathVariable annonsenr: String): ResponseEntity<RekrutteringsbistandStilling> {
        return ok(stillingService.hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr))
    }

    @GetMapping("/mine-stillinger")
    fun hentMineStillinger(request: HttpServletRequest): ResponseEntity<Page<RekrutteringsbistandStilling>> {

        val queryString = if (request.queryString != null) {
            URLDecoder.decode(request.queryString, StandardCharsets.UTF_8)
        } else {
            null
        }

        val stillinger: Page<RekrutteringsbistandStilling> = stillingService.hentMineStillinger(queryString)

        return ok(stillinger)
    }
}
