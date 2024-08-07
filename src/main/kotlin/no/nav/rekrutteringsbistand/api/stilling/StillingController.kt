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
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsinfo
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori


@RestController
@Protected
class StillingController(private val stillingsinfoService: StillingsinfoService, private val stillingService: StillingService, private val tokenUtils: TokenUtils) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        if(stilling.kategori==Stillingskategori.FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        }
        val opprettetStilling = stillingService.opprettNyStilling(stilling)
        return ok(opprettetStilling)
    }

    @PostMapping("/rekrutteringsbistandstilling/kopier/{stillingsId}")
    fun kopierStilling(@PathVariable stillingsId: String): ResponseEntity<RekrutteringsbistandStilling> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        val kopiertStilling = stillingService.kopierStilling(stillingsId)
        return ok(kopiertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun oppdaterStilling(request: HttpServletRequest, @RequestBody rekrutteringsbistandStillingDto: OppdaterRekrutteringsbistandStillingDto): ResponseEntity<OppdaterRekrutteringsbistandStillingDto> {
        val stillingskategori = stillingsinfoService.hentForStilling(Stillingsid(rekrutteringsbistandStillingDto.stilling.uuid))
            .map(Stillingsinfo::stillingskategori)
            .getOrElse { Stillingskategori.STILLING }
        if(stillingskategori==Stillingskategori.FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        }
        val oppdatertStilling = stillingService.oppdaterRekrutteringsbistandStilling(rekrutteringsbistandStillingDto, request.queryString)
        return ok(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistandstilling/{stillingsId}")
    fun slettRekrutteringsbistandStilling(@PathVariable(value = "stillingsId") stillingsId: String): ResponseEntity<Stilling> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
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
