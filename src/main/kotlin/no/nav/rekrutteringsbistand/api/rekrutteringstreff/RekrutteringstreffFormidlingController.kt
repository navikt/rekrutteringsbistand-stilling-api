package no.nav.rekrutteringsbistand.api.rekrutteringstreff

import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.OpprettRekrutteringstreffFormidling
import no.nav.rekrutteringsbistand.api.rekrutteringstreff.dto.OpprettRekrutteringstreffFormidlingRespons
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
@Protected
class RekrutteringstreffFormidlingController(
    private val rekrutteringstreffFormidlingService: RekrutteringstreffFormidlingService,
    private val tokenUtils: TokenUtils,
){
    @PostMapping("/rekrutteringstreff/formidling")
    fun opprettFormidlingRekrutteringstreff(@RequestBody rekrutteringstreffFormidling: OpprettRekrutteringstreffFormidling): ResponseEntity<OpprettRekrutteringstreffFormidlingRespons> {
        val innloggetVeileder = tokenUtils.hentInnloggetVeileder()
        innloggetVeileder.validerMinstEnAvRollene(Rolle.JOBBSØKERRETTET, Rolle.ARBEIDSGIVERRETTET)

        val navIdent = innloggetVeileder.navIdent
        val displayName = innloggetVeileder.displayName
        val eierNavKontorEnhetId = rekrutteringstreffFormidling.eierNavKontorEnhetId

        val opprettetFormidling = rekrutteringstreffFormidlingService.opprettRekrutteringsbistandFormidling(
            eierNavIdent = navIdent,
            eierNavn = displayName,
            eierNavKontorEnhetId = eierNavKontorEnhetId,
            rekrutteringstreffId = rekrutteringstreffFormidling.rekrutteringstreffId,
            stilling = rekrutteringstreffFormidling.stilling
        )
        return ResponseEntity.ok(
            OpprettRekrutteringstreffFormidlingRespons(
                kandidatlisteId = opprettetFormidling.kandidatlisteId,
                stillingsId = opprettetFormidling.stillingsId
            )
        )
    }
}