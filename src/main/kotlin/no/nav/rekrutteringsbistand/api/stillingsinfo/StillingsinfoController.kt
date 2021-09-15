package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/stillingsinfo")
@ProtectedWithClaims(issuer = "isso")
class StillingsinfoController(
    val repo: StillingsinfoRepository,
    val service: StillingsinfoService,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient
) {
    @PutMapping
    fun overtaEierskapForEksternStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<StillingsinfoDto> {
        val oppdatertStillingsinfo = service.overtaEierskapForEksternStilling(
            stillingsId = dto.stillingsid,
            eier = Eier(dto.eierNavident, dto.eierNavn)
        )

        kandidatlisteKlient.varsleOmOppdatertStilling(Stillingsid(dto.stillingsid))
        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(dto.stillingsid)

        return ResponseEntity.status(HttpStatus.OK).body(oppdatertStillingsinfo.asStillingsinfoDto())
    }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<StillingsinfoDto> =
        repo.hentForIdent(id).map { it.asStillingsinfoDto() }
}

data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavident: String?,
    val eierNavn: String?
) 
