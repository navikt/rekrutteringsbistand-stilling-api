package no.nav.rekrutteringsbistand.api.requester

import no.nav.security.oidc.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.*
import java.sql.ResultSet
import java.util.*

@RestController
@RequestMapping("/rekruttering")
@Protected
class RekrutteringsbistandController(val repo: RekrutteringsbistandRepository) {

    @PostMapping
    fun lagre(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {

        if(dto.rekrutteringUuid != null)  return ResponseEntity.badRequest().body(dto)

        repo.lagre(Rekrutteringsbistand(
                rekrutteringUuid = UUID.randomUUID().toString(),
                stillingUuid = dto.stillingUuid,
                overfoertTil = dto.overfoertTil
        ))
        return ResponseEntity.ok().body(dto)
    }

    @PutMapping
    fun oppdater(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {
        dto.rekrutteringUuid ?: return ResponseEntity.badRequest().body(dto)

        repo.oppdater(Rekrutteringsbistand(
                rekrutteringUuid = dto.rekrutteringUuid,
                stillingUuid = dto.stillingUuid,
                overfoertTil = dto.overfoertTil
        ))
        return ResponseEntity.ok().body(dto)
    }


    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): RekrutteringsbistandDto =
            repo.hentForStilling(id)
                    .run {
                        RekrutteringsbistandDto(
                                rekrutteringUuid = this.rekrutteringUuid,
                                stillingUuid = this.stillingUuid,
                                overfoertTil = this.overfoertTil)
                    }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<RekrutteringsbistandDto> =
            repo.hentForIdent(id)
                    .run {
                        this.map {
                            RekrutteringsbistandDto(
                                    rekrutteringUuid = it.rekrutteringUuid,
                                    stillingUuid = it.stillingUuid,
                                    overfoertTil = it.overfoertTil)
                        }
                    }

    data class RekrutteringsbistandDto(
            val rekrutteringUuid: String?,
            val stillingUuid: String,
            val overfoertTil: String
    )
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
                            Pair("overfoert_til", rekrutteringsbistand.overfoertTil)
                    )
            )

    fun oppdater(rekrutteringsbistand: Rekrutteringsbistand) =
            namedParameterJdbcTemplate.update(
                    "update Rekrutteringsbistand set overfoert_til=:overfoert_til where rekruttering_uuid=:rekruttering_uuid",
                    mapOf(
                            Pair("rekruttering_uuid", rekrutteringsbistand.rekrutteringUuid),
                            Pair("overfoert_til", rekrutteringsbistand.overfoertTil)
                    )

            )

    fun hentForStilling(stillingUuid: String): Rekrutteringsbistand =
            namedParameterJdbcTemplate.queryForObject(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid = :stilling_uuid",
                    MapSqlParameterSource("stilling_uuid", stillingUuid))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        overfoertTil = rs.getString("overfoert_til"))
            }!!

    fun hentForIdent(ident: String): Collection<Rekrutteringsbistand> =
            namedParameterJdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE overfoert_til = :overfoert_til",
                    MapSqlParameterSource("overfoert_til", ident))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        overfoertTil = rs.getString("overfoert_til")
                )
            }
}

data class Rekrutteringsbistand(
        val rekrutteringUuid: String,
        val stillingUuid: String,
        val overfoertTil: String
)
