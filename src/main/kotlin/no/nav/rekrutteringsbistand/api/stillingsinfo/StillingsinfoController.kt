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

    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): StillingsinfoDto =
        repo.hentForStilling(Stillingsid(id)).map { it.asStillingsinfoDto() }
            .getOrElse { throw NotFoundException("Stilling id $id") }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<StillingsinfoDto> =
        repo.hentForIdent(id).map { it.asStillingsinfoDto() }
}

data class StillingsinfoInboundDto(
    val stillingsid: String,
    val eierNavident: String?,
    val eierNavn: String?
) {
    fun tilOpprettetStillingsinfo() = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        stillingsid = Stillingsid(verdi = stillingsid),
        eier = Eier(navident = eierNavident, navn = eierNavn),
        notat = null
    )

    fun tilOppdatertStillingsinfo(stillingsinfoId: String, notat: String?) = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(stillingsinfoId),
        stillingsid = Stillingsid(verdi = stillingsid),
        eier = Eier(navident = eierNavident, navn = eierNavn),
        notat = notat
    )
}

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)
