package no.nav.rekrutteringsbistand.api.stillingsinfo

import arrow.core.getOrElse
import no.nav.security.oidc.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*

@RestController
@RequestMapping("/rekruttering")
@Protected
class StillingsinfoController(val repo: StillingsinfoRepository) {

    @PostMapping
    fun lagre(@RequestBody dto: StillingsinfoDto): ResponseEntity<StillingsinfoDto> {

        if (dto.stillingsinfoid != null) throw BadRequestException("stillingsinfoid må være tom for post")
        val medUuid = dto.copy(stillingsinfoid = UUID.randomUUID().toString())
        repo.lagre(medUuid.asStillingsinfo())
        return ResponseEntity.created(URI("/rekruttering/${medUuid.stillingsinfoid}")).body(medUuid)
    }

    @PutMapping
    fun oppdater(@RequestBody dto: StillingsinfoDto): ResponseEntity<StillingsinfoDto> {
        if (dto.stillingsinfoid == null) throw BadRequestException("Stillingsinfoid må ha verdi for put")

        repo.oppdaterEierIdentOgEierNavn(OppdaterStillingsinfo(
                stillingsinfoid = Stillingsinfoid(dto.stillingsinfoid),
                eier = Eier(navident = dto.eierNavident, navn = dto.eierNavn)
        ))
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
