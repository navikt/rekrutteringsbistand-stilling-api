package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.KopierStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stillingsinfo.Eier
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingsid
import no.nav.rekrutteringsbistand.api.stillingsinfo.StillingsinfoService
import no.nav.rekrutteringsbistand.api.stillingsinfo.Stillingskategori
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@Protected
class StillingController(
    private val stillingsinfoService: StillingsinfoService,
    private val stillingService: StillingService,
    private val tokenUtils: TokenUtils,
) {

    @PostMapping("/rekrutteringsbistandstilling")
    fun opprettStilling(@RequestBody stilling: OpprettRekrutteringsbistandstillingDto): ResponseEntity<RekrutteringsbistandStilling> {
        if (stilling.kategori == Stillingskategori.FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else if (stilling.kategori == Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Kan ikke opprette stilling med kategori REKRUTTERINGSTREFF_FORMIDLING med dette endepunktet")
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.ARBEIDSGIVERRETTET)
        }
        val opprettetStilling = stillingService.opprettNyStilling(stilling, tokenUtils)
        return ok(opprettetStilling)
    }

    @PostMapping("/rekrutteringsbistandstilling/kopier/{stillingsId}")
    fun kopierStilling(
        @PathVariable stillingsId: UUID,
        @RequestBody kopierStillingDto: KopierStillingDto?
    ): ResponseEntity<RekrutteringsbistandStilling> {
        val innloggetVeileder = tokenUtils.hentInnloggetVeileder()
        innloggetVeileder.validerMinstEnAvRolleneEllerUtvikler(Rolle.ARBEIDSGIVERRETTET)
        val navIdent = innloggetVeileder.navIdent
        val displayName = innloggetVeileder.displayName
        val eierNavKontorEnhetId = kopierStillingDto?.eierNavKontorEnhetId

        val kopiertStilling = stillingService.kopierStilling(stillingsId, navIdent, displayName, eierNavKontorEnhetId)
        return ok(kopiertStilling)
    }

    @PutMapping("/rekrutteringsbistandstilling")
    fun oppdaterStilling(
        @RequestBody rekrutteringsbistandStillingDto: RekrutteringsbistandStilling
    ): ResponseEntity<RekrutteringsbistandStilling> {
        val stillingskategori = stillingsinfoService.hentStillingsinfo(
            Stillingsid(rekrutteringsbistandStillingDto.stilling.uuid)
        )?.stillingskategori ?: Stillingskategori.STILLING
        if (stillingskategori == Stillingskategori.FORMIDLING || stillingskategori == Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.ARBEIDSGIVERRETTET)
        }
        val veileder = tokenUtils.hentInnloggetVeileder()
        val eier = Eier(
            navn = veileder.displayName,
            navident = veileder.navIdent,
            navKontorEnhetId = rekrutteringsbistandStillingDto.stillingsinfo?.eierNavKontorEnhetId
        )

        val oppdatertStilling =
            stillingService.oppdaterRekrutteringsbistandStilling(dto = rekrutteringsbistandStillingDto, eier = eier)
        return ok(oppdatertStilling)
    }

    @DeleteMapping("/rekrutteringsbistandstilling/{stillingsId}")
    fun slettRekrutteringsbistandStilling(@PathVariable(value = "stillingsId") stillingsIdSomUuid: UUID): ResponseEntity<FrontendStilling> {
        val stillingsId = Stillingsid(stillingsIdSomUuid)
        val stillingskategori =
            stillingsinfoService.hentStillingsinfo(stillingsId)?.stillingskategori ?: Stillingskategori.STILLING
        if (stillingskategori == Stillingskategori.FORMIDLING || stillingskategori == Stillingskategori.REKRUTTERINGSTREFF_FORMIDLING) {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)
        } else {
            tokenUtils.hentInnloggetVeileder().validerMinstEnAvRolleneEllerUtvikler(Rolle.ARBEIDSGIVERRETTET)
        }
        val slettetStilling = stillingService.slettRekrutteringsbistandStilling(stillingsIdSomUuid)
        return ok(slettetStilling)
    }

    @GetMapping("/rekrutteringsbistandstilling/{uuid}")
    fun hentRekrutteringsbistandStilling(@PathVariable uuid: UUID): ResponseEntity<RekrutteringsbistandStilling> {
        // TODO styrk(kan tas til slutt): Interne stillinger burde ikke sende tittel.
        return ok(stillingService.hentRekrutteringsbistandStilling(uuid))
    }
}
