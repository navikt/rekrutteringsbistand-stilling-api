package no.nav.rekrutteringsbistand.api.stillingsinfo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.AuditLogg
import no.nav.rekrutteringsbistand.api.autorisasjon.Rolle
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.stilling.DirektemeldtStillingService
import no.nav.rekrutteringsbistand.api.support.log
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@RestController
@Protected
class StillingsinfoController(
    val service: StillingsinfoService,
    val direktemeldtStillingService: DirektemeldtStillingService,
    val tokenUtils: TokenUtils
) {
    @PutMapping("/stillingsinfo")
    fun overtaEierskapForEksternStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<StillingsinfoDto> {
        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        val stillingsid = Stillingsid(dto.stillingsid)
        val veileder = tokenUtils.hentInnloggetVeileder()

        val forrigeStillingsinfo = service.hentStillingsinfo(stillingId = stillingsid)
        val forrigeEier = forrigeStillingsinfo?.eier?.navident

        log.info("Stilling ${dto.stillingsid} har byttet eierskap med url /stillingsinfo")
        AuditLogg.loggOvertattStilling(navIdent = veileder.navIdent, forrigeEier=forrigeEier, stillingsid=dto.stillingsid)
        val nyEier = Eier(veileder.navIdent, veileder.displayName, dto.eierNavKontorEnhetId)
        val oppdatertStillingsinfo =
            service.overtaEierskapForEksternStillingOgKandidatliste(stillingsId = stillingsid, nyEier = nyEier)

        return ResponseEntity.status(HttpStatus.OK).body(oppdatertStillingsinfo.asStillingsinfoDto())
    }

    @PutMapping("/overta-eierskap")
    fun overtaEierskapForStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<String> {
        val stillingsid = try {
            Stillingsid(dto.stillingsid)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig UUID", e)
        }

        tokenUtils.hentInnloggetVeileder().validerMinstEnAvRollene(Rolle.ARBEIDSGIVERRETTET)
        val stilling = direktemeldtStillingService.hentDirektemeldtStilling(stillingsid)
        val veileder = tokenUtils.hentInnloggetVeileder()

        val forrigeStillingsinfo = service.hentStillingsinfo(stillingId = stillingsid)
        val forrigeEier = forrigeStillingsinfo?.eier?.navident

        if (forrigeStillingsinfo?.stillingskategori == Stillingskategori.FORMIDLING) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Kan ikke endre eier på formidlingsstillinger")
        }

        if(stilling != null) {
            val nyEier = Eier(navident = veileder.navIdent, navn = veileder.displayName, navKontorEnhetId = dto.eierNavKontorEnhetId)
            direktemeldtStillingService.overtaEierskapForStillingOgKandidatliste(stillingsinfo = dto, stilling = stilling, nyEier = nyEier)
        } else {
            val nyEier = Eier(veileder.navIdent, veileder.displayName, dto.eierNavKontorEnhetId)
            service.overtaEierskapForEksternStillingOgKandidatliste(stillingsId = stillingsid, nyEier = nyEier)
        }

        AuditLogg.loggOvertattStilling(navIdent = veileder.navIdent, forrigeEier=forrigeEier, stillingsid=dto.stillingsid)
        log.info("Stilling ${dto.stillingsid} har byttet eierskap med url /overta-eierskap")
        return ResponseEntity.status(HttpStatus.OK).body("OK")
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavKontorEnhetId: String?,
)
