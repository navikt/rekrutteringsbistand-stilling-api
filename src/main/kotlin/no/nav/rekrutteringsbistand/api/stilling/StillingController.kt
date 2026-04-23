package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import jakarta.servlet.http.HttpServletRequest
import no.nav.rekrutteringsbistand.api.KopierStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.AuthorizedPartyUtils
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Eier
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@Protected
class StillingController(private val stillingsinfoService: StillingsinfoService, private val stillingService: StillingService, private val tokenUtils: TokenUtils, private val authorizedPartyUtils: AuthorizedPartyUtils) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        if(stilling.kategori==Stillingskategori.FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        }
        val opprettetStilling = stillingService.opprettNyStilling(stilling, tokenUtils)
        return ok(opprettetStilling)
    }

    @PostMapping("/rekrutteringsbistandstilling/kopier/{stillingsId}")
    fun kopierStilling(@PathVariable stillingsId: String, @RequestBody kopierStillingDto: KopierStillingDto?): ResponseEntity<RekrutteringsbistandStilling> {
        val innloggetVeileder = tokenUtils.hentInnloggetVeileder()
        innloggetVeileder.validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        val navIdent = innloggetVeileder.navIdent
        val displayName = innloggetVeileder.displayName
        val eierNavKontorEnhetId = kopierStillingDto?.eierNavKontorEnhetId

        val kopiertStilling = stillingService.kopierStilling(stillingsId, navIdent,  displayName, eierNavKontorEnhetId)
        return ok(kopiertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun oppdaterStilling(request: HttpServletRequest, @RequestBody rekrutteringsbistandStillingDto: RekrutteringsbistandStilling): ResponseEntity<RekrutteringsbistandStilling> {
        val stillingskategori = stillingsinfoService.hentStillingsinfo(
            Stillingsid(rekrutteringsbistandStillingDto.stilling.uuid)
        )?.stillingskategori ?: Stillingskategori.STILLING
        if(stillingskategori==Stillingskategori.FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        }
        val veileder = tokenUtils.hentInnloggetVeileder()
        val eier = Eier(navn = veileder.displayName, navident = veileder.navIdent, navKontorEnhetId = rekrutteringsbistandStillingDto.stillingsinfo?.eierNavKontorEnhetId)

        val oppdatertStilling = stillingService.oppdaterRekrutteringsbistandStilling(dto = rekrutteringsbistandStillingDto, eier = eier)
        return ok(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistandstilling/{stillingsId}")
    fun slettRekrutteringsbistandStilling(@PathVariable(value = "stillingsId") stillingsId: String): ResponseEntity<FrontendStilling> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        val slettetStilling = stillingService.slettRekrutteringsbistandStilling(stillingsId)
        return ok(slettetStilling)
    }

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: String): ResponseEntity<RekrutteringsbistandStilling> {
        // TODO styrk(kan tas til slutt): Interne stillinger burde ikke sende tittel.
        try {
            UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig uuid", e)
        }

        return ok(stillingService.hentRekrutteringsbistandStilling(uuid))
    }
}
