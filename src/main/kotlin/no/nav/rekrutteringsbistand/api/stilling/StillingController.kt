package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import jakarta.servlet.http.HttpServletRequest
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.minestillinger.MineStillingerService


@RestController
@Protected
class StillingController(
    val stillingService: StillingService,
    val mineStillingerService: MineStillingerService,
    val tokenUtils: TokenUtils
) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        val opprettetStilling = stillingService.opprettStilling(stilling)
        mineStillingerService.opprett(opprettetStilling.stilling, tokenUtils.hentNavIdent())
        return ok(opprettetStilling)
    }

    @PostMapping("/rekrutteringsbistandstilling/kopier/{stillingsId}")
    fun kopierStilling(@PathVariable stillingsId: String): ResponseEntity<RekrutteringsbistandStilling> {
        val kopiertStilling = stillingService.kopierStilling(stillingsId)
        mineStillingerService.opprett(kopiertStilling.stilling, tokenUtils.hentNavIdent())
        return ok(kopiertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun oppdaterStilling(
        request: HttpServletRequest,
        @RequestBody rekrutteringsbistandStillingDto: OppdaterRekrutteringsbistandStillingDto
    ): ResponseEntity<OppdaterRekrutteringsbistandStillingDto> {
        val oppdatertStilling =
            stillingService.oppdaterRekrutteringsbistandStilling(rekrutteringsbistandStillingDto, request.queryString)
        mineStillingerService.oppdater(oppdatertStilling.stilling, tokenUtils.hentNavIdent())
        return ok(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistandstilling/{stillingsId}")
    fun slettRekrutteringsbistandStilling(@PathVariable(value = "stillingsId") stillingsId: String): ResponseEntity<Stilling> {
        val slettetStilling = stillingService.slettRekrutteringsbistandStilling(stillingsId)
        return ok(slettetStilling)
    }

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: String): ResponseEntity<RekrutteringsbistandStilling> {
        return ok(stillingService.hentRekrutteringsbistandStilling(uuid))
    }

    @GetMapping("/rekrutteringsbistandstilling/annonsenr/{annonsenr}")
    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(@PathVariable annonsenr: String): ResponseEntity<RekrutteringsbistandStilling> {
        val stilling = stillingService.hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr)
        return stilling.map { ok(it) }.getOrElse { notFound().build() }
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
