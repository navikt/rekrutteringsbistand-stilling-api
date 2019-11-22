package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.support.LOG
import no.nav.security.oidc.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/rekruttering")
@Protected
class StillingsinfoController(
        val repo: StillingsinfoRepository,
        val kandidatlisteKlient: KandidatlisteKlient
) {

    @PostMapping
    fun lagre(@RequestBody dto: StillingsinfoDto): ResponseEntity<StillingsinfoDto> {
        if (dto.stillingsinfoid != null) throw BadRequestException("stillingsinfoid må være tom for post")

        val stillingsInfo = dto.copy(stillingsinfoid = UUID.randomUUID().toString()).asStillingsinfo()
        LOG.debug("lager ny stillingsinfo for stillinginfoid ${stillingsInfo.stillingsid} stillingid ${stillingsInfo.stillingsinfoid}")

        repo.lagre(stillingsInfo)
        kandidatlisteKlient.sendAdCandidateListMessage(stillingsInfo.stillingsid)
        return ResponseEntity.created(URI("/rekruttering/${stillingsInfo.stillingsinfoid.asString()}")).body(stillingsInfo.asDto())
    }

    @PutMapping
    fun oppdater(@RequestBody dto: StillingsinfoDto): ResponseEntity<StillingsinfoDto> {
        if (dto.stillingsinfoid == null) throw BadRequestException("Stillingsinfoid må ha verdi for put")

        LOG.debug("Oppdaterer stillingsinfo for stillingInfoid ${dto.asStillingsinfo().stillingsinfoid.asString()} stillingid  ${dto.asStillingsinfo().stillingsid.asString()}")
        repo.oppdaterEierIdentOgEierNavn(dto.asOppdaterStillingsinfo())
        kandidatlisteKlient.sendAdCandidateListMessage(dto.asStillingsinfo().stillingsid)
        return ResponseEntity.ok().body(dto)
    }

    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): StillingsinfoDto =
            repo.hentForStilling(Stillingsid(id)).map { it.asDto() }.getOrElse { throw NotFoundException("Stilling id $id") }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<StillingsinfoDto> =
            repo.hentForIdent(id).map { it.asDto() }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)
