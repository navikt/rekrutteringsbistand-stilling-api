package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.extensions.either.foldable.isEmpty
import arrow.core.extensions.either.foldable.isNotEmpty
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/rekruttering")
@ProtectedWithClaims(issuer = "isso")
class StillingsinfoController(
    val repo: StillingsinfoRepository,
    val service: StillingsinfoService,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient
) {
    @PutMapping
    fun endreEierForEksternStillingOgKandidatliste(
        @RequestBody dto: StillingsinfoInboundDto
    ): ResponseEntity<StillingsinfoDto> {
        val oppdatertStillingsinfo = service.endreEier(dto)

        kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(dto.stillingsid))
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
