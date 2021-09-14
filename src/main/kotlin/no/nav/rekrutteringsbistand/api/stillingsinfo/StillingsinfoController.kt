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
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient
) {
    @PostMapping("/kandidatliste")
    fun opprettKandidatlisteForEksternStilling(@RequestBody dto: OpprettKandidatlisteForEksternStillingDto): ResponseEntity<StillingsinfoDto> {
        val eksisterendeStillingsinfo = repo.hentForStilling(Stillingsid(dto.stillingsid))

        val opprettetStillingsinfo: Stillingsinfo =
            if (eksisterendeStillingsinfo is Some) {
                dto.tilOppdaterStillingsinfo(
                    eksisterendeStillingsinfo.get().stillingsinfoid.asString()
                ).apply {
                    repo.oppdaterEierIdentOgEierNavn(
                        OppdaterEier(this.stillingsinfoid, Eier(dto.eierNavident, dto.eierNavn)),
                    )
                }
            } else {
                dto.tilOpprettStillingsinfo().apply {
                    repo.opprett(this)
                }
            }

        kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(dto.stillingsid))
        arbeidsplassenKlient.triggResendingAvStillingsmeldingFraArbeidsplassen(dto.stillingsid)

        return ResponseEntity.status(HttpStatus.CREATED).body(opprettetStillingsinfo.asStillingsinfoDto())
    }

    @PostMapping
    fun lagre(@RequestBody dto: EierDto): ResponseEntity<EierDto> {
        if (dto.stillingsinfoid != null) throw BadRequestException("stillingsinfoid må være tom for post")

        return repo.hentForStilling(Stillingsid(dto.stillingsid))
            .map {
                oppdater(dto.copy(stillingsinfoid = it.asEierDto().stillingsinfoid))
            }.getOrElse {
                val dtoMedId = dto.copy(stillingsinfoid = UUID.randomUUID().toString())
                LOG.debug("Lager ny eierinformasjon for stilling ${dtoMedId.stillingsid} med stillingsInfoId ${dtoMedId.stillingsinfoid}")

                repo.opprett(dtoMedId.asStillinginfo())
                kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(dto.stillingsid))
                ResponseEntity.created(URI("/rekruttering/${dtoMedId.stillingsinfoid}")).body(dtoMedId)
            }
    }

    @PutMapping
    fun oppdater(@RequestBody dto: EierDto): ResponseEntity<EierDto> {
        if (dto.stillingsinfoid == null) throw BadRequestException("Stillingsinfoid må ha verdi for put")

        LOG.debug("Oppdaterer eierinformasjon for stillingInfoid ${dto.asStillinginfo().stillingsinfoid.asString()} stillingid  ${dto.asStillinginfo().stillingsid.asString()}")
        repo.oppdaterEierIdentOgEierNavn(dto.asOppdaterEierinfo())
        kandidatlisteKlient.oppdaterKandidatliste(dto.asStillinginfo().stillingsid)
        return ResponseEntity.ok().body(dto)
    }

    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): EierDto =
        repo.hentForStilling(Stillingsid(id)).map { it.asEierDto() }
            .getOrElse { throw NotFoundException("Stilling id $id") }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<EierDto> =
        repo.hentForIdent(id).map { it.asEierDto() }

}

data class OpprettKandidatlisteForEksternStillingDto(
    val stillingsid: String,
    val eierNavident: String?,
    val eierNavn: String?
) {
    fun tilOpprettStillingsinfo() = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
        stillingsid = Stillingsid(verdi = stillingsid),
        eier = Eier(navident = eierNavident, navn = eierNavn),
        notat = null
    )

    fun tilOppdaterStillingsinfo(stillingsinfoId: String) = Stillingsinfo(
        stillingsinfoid = Stillingsinfoid(stillingsinfoId),
        stillingsid = Stillingsid(verdi = stillingsid),
        eier = Eier(navident = eierNavident, navn = eierNavn),
        notat = null
    )
}


@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)
