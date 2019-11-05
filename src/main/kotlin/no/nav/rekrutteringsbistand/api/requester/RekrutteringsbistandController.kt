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
                eierIdent = dto.eierIdent,
                eierNavn = dto.eierNavn
        ))
        return ResponseEntity.ok().body(dto)
    }

    @PutMapping
    fun oppdater(@RequestBody dto: RekrutteringsbistandDto): ResponseEntity<RekrutteringsbistandDto> {
        dto.rekrutteringUuid ?: return ResponseEntity.badRequest().body(dto)

        repo.oppdater(Rekrutteringsbistand(
                rekrutteringUuid = dto.rekrutteringUuid,
                stillingUuid = dto.stillingUuid,
                eierIdent = dto.eierIdent,
                eierNavn =  dto.eierNavn
        ))
        return ResponseEntity.ok().body(dto)
    }

    @GetMapping("/stilling")
    fun hentForStillingider(@RequestParam stillingUuider: List<String>): List<RekrutteringsbistandDto> =
            repo.hentForStillinger(stillingUuider)
                    .map {     RekrutteringsbistandDto(
                            rekrutteringUuid = it.rekrutteringUuid,
                            stillingUuid = it.stillingUuid,
                            eierIdent = it.eierIdent,
                            eierNavn = it.eierNavn) }


    @GetMapping("/stilling/{id}")
    fun hentForStilling(@PathVariable id: String): RekrutteringsbistandDto =
            repo.hentForStilling(id)
                    .run {
                        RekrutteringsbistandDto(
                                rekrutteringUuid = this.rekrutteringUuid,
                                stillingUuid = this.stillingUuid,
                                eierIdent = this.eierIdent,
                                eierNavn = this.eierNavn)
                    }

    @GetMapping("/ident/{id}")
    fun hentForIdent(@PathVariable id: String): Collection<RekrutteringsbistandDto> =
            repo.hentForIdent(id)
                    .run {
                        this.map {
                            RekrutteringsbistandDto(
                                    rekrutteringUuid = it.rekrutteringUuid,
                                    stillingUuid = it.stillingUuid,
                                    eierIdent = it.eierIdent,
                                    eierNavn = it.eierNavn)
                        }
                    }

    data class RekrutteringsbistandDto(
            val rekrutteringUuid: String?,
            val stillingUuid: String,
            val eierIdent: String,
            val eierNavn: String
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
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        eierIdent = rs.getString("eier_ident"),
                        eierNavn = rs.getString("eier_navn")

                )
            }!!

    fun hentForStillinger(stillingUuider: List<String>): List<Rekrutteringsbistand> =
        namedParameterJdbcTemplate.query(
                "SELECT * FROM REKRUTTERINGSBISTAND WHERE stilling_uuid IN(:stilling_uuider)",
                MapSqlParameterSource("stilling_uuider", stillingUuider.joinToString(",")))
        { rs: ResultSet, _: Int ->
            Rekrutteringsbistand(
                    rekrutteringUuid = rs.getString("rekruttering_uuid"),
                    stillingUuid = rs.getString("stilling_uuid"),
                    eierIdent = rs.getString("eier_ident"),
                    eierNavn = rs.getString("eier_navn")
            )
    }

    fun hentForIdent(ident: String): Collection<Rekrutteringsbistand> =
            namedParameterJdbcTemplate.query(
                    "SELECT * FROM REKRUTTERINGSBISTAND WHERE eier_ident = :eier_ident",
                    MapSqlParameterSource("eier_ident", ident))
            { rs: ResultSet, _: Int ->
                Rekrutteringsbistand(
                        rekrutteringUuid = rs.getString("rekruttering_uuid"),
                        stillingUuid = rs.getString("stilling_uuid"),
                        eierIdent = rs.getString("eier_ident"),
                        eierNavn = rs.getString("eier_navn")
                )
            }
}

data class Rekrutteringsbistand(
        val rekrutteringUuid: String,
        val stillingUuid: String,
        val eierIdent: String,
        val eierNavn: String
)
