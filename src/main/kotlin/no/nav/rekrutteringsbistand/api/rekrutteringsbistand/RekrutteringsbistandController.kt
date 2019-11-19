package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

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
class RekrutteringsbistandController(val repo: RekrutteringsbistandRepository) {

    @PostMapping
    fun lagre(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {

        if (dto.rekrutteringUuid != null) throw BadRequestException("rekrutteringsUuid must be null for post")
        val medUuid = dto.copy(rekrutteringUuid = UUID.randomUUID().toString())
        repo.lagre(medUuid.asRekrutteringsbistand())
        return ResponseEntity.created(URI("/rekruttering/${medUuid.rekrutteringUuid}")).body(medUuid)
    }

    @PutMapping
    fun oppdater(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {
        if (dto.rekrutteringUuid == null) throw BadRequestException("rekrutteringUuid must not be null for put")

        repo.oppdaterEierIdentOgEierNavn(OppdaterRekrutteringsbistand(
                rekrutteringsUuid = dto.rekrutteringUuid,
                eierIdent = dto.eierIdent,
                eierNavn = dto.eierNavn
        ))
        return ResponseEntity.ok().body(dto)
    }

    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): RekrutteringsbistandDto =
            repo.hentForStilling(StillingId(id)).map { it.asDto() }.getOrElse { throw NotFoundException("Stilling id $id") }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<RekrutteringsbistandDto> =
            repo.hentForIdent(id).map { it.asDto() }

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String) : RuntimeException(message)
