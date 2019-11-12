package no.nav.rekrutteringsbistand.api.rekrutteringsbistand

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.security.oidc.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.lang.RuntimeException
import java.sql.ResultSet
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
        return ResponseEntity.ok().body(medUuid)
    }

    @PutMapping
    fun oppdater(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {
        dto.rekrutteringUuid ?: throw BadRequestException("rekrutteringUuid must not be null for put")

        repo.oppdater(Rekrutteringsbistand(
                rekrutteringUuid = dto.rekrutteringUuid,
                stillingUuid = dto.stillingUuid,
                eierIdent = dto.eierIdent,
                eierNavn = dto.eierNavn
        ))
        return ResponseEntity.ok().body(dto)
    }

    @GetMapping("/stilling")
    fun hentForStillingider(@RequestParam stillingUuider: List<String>): List<RekrutteringsbistandDto> =
            repo.hentForStillinger(stillingUuider)
                    .map { it.asDto() }


    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): RekrutteringsbistandDto =
            repo.hentForStilling(id)
                    .run {
                        this.asDto()
                    }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<RekrutteringsbistandDto> =
            repo.hentForIdent(id)
                    .run {
                        this.map {
                            it.asDto()
                        }
                    }
}

@Service
class RekrutteringsbistandService(val repo: RekrutteringsbistandRepository) {

    fun hentForStilling(uuid: String): Rekrutteringsbistand = repo.hentForStilling(uuid)
}

@Repository
class RekrutteringsbistandRepository(
        val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
        simpleJdbcInsert: SimpleJdbcInsert) {

    val rekrutteringsbistandInsert = simpleJdbcInsert.withTableName("REKRUTTERINGSBISTAND").usingGeneratedKeyColumns("ID")

    fun lagre(rekrutteringsbistand: Rekrutteringsbistand) =
            rekrutteringsbistandInsert.executeAndReturnKey(
                    mapOf(
                            Pair("rekruttering_uuid", rekrutteringsbistand.rekrutteringUuid),
                            Pair("stilling_uuid", rekrutteringsbistand.stillingUuid),
                            Pair("eier_ident", rekrutteringsbistand.eierIdent),
                            Pair("eier_navn", rekrutteringsbistand.eierNavn)
                    )
            )

    fun oppdater(rekrutteringsbistand: Rekrutteringsbistand) =
            namedParameterJdbcTemplate.update(
                    "update Rekrutteringsbistand set eier_ident=:eier_ident, eier_navn=:eier_navn where rekruttering_uuid=:rekruttering_uuid",
                    mapOf(
                            Pair("rekruttering_uuid", rekrutteringsbistand.rekrutteringUuid),
                            Pair("eier_ident", rekrutteringsbistand.eierIdent),
                            Pair("eier_navn", rekrutteringsbistand.eierNavn)

                    )

            )

    fun hentForStilling(stillingUuid: String): Rekrutteringsbistand =
            namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid = :stilling_uuid",
                    MapSqlParameterSource("stilling_uuid", stillingUuid))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }!!

    fun hentForStillinger(stillingUuider: List<String>): List<Rekrutteringsbistand> =
            namedParameterJdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid IN(:stilling_uuider)",
                    MapSqlParameterSource("stilling_uuider", stillingUuider.joinToString(",")))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }

    fun hentForIdent(ident: String): Collection<Rekrutteringsbistand> =
            namedParameterJdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE eier_ident = :eier_ident",
                    MapSqlParameterSource("eier_ident", ident))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand.fromDB(rs)
            }
}

data class Rekrutteringsbistand(
        val rekrutteringUuid: String?,
        val stillingUuid: String,
        val eierIdent: String,
        val eierNavn: String
) {
    fun asDto() =
            RekrutteringsbistandDto(
                    rekrutteringUuid = this.rekrutteringUuid,
                    stillingUuid = this.stillingUuid,
                    eierIdent = this.eierIdent,
                    eierNavn = this.eierNavn)

    companion object {
        fun fromDB(rs: ResultSet) =
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        eierIdent = rs.getString("eier_ident"),
                        eierNavn = rs.getString("eier_navn"))
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RekrutteringsbistandDto(
        val rekrutteringUuid: String?,
        val stillingUuid: String,
        val eierIdent: String,
        val eierNavn: String
) {
    fun asRekrutteringsbistand() =
            Rekrutteringsbistand(
                    rekrutteringUuid = this.rekrutteringUuid,
                    stillingUuid = this.stillingUuid,
                    eierIdent = this.eierIdent,
                    eierNavn = this.eierNavn
            )

}

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException : RuntimeException {
    constructor(message: String) : super(message)
}
