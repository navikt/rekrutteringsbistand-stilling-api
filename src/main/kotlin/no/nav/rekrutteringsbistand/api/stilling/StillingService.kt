package no.nav.rekrutteringsbistand.api.stilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.rekrutteringsbistand.AuditLogg
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


@Service
class StillingService(
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient,
    val direktemeldtStillingRepository: DirektemeldtStillingRepository,
    val stillingssokProxyClient: StillingssokProxyClient
) {
    fun hentRekrutteringsbistandStilling(
        stillingsId: String,
        somSystembruker: Boolean = false
    ): RekrutteringsbistandStilling {
        val stillingsinfo = stillingsinfoService.hentForStilling(Stillingsid(stillingsId))?.asStillingsinfoDto()

        direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)?.let { direktemeldtStilling ->
            log.info("Hentet stilling fra databasen $stillingsId")
            return RekrutteringsbistandStilling(
                stilling = direktemeldtStilling.toStilling(),
                stillingsinfo = stillingsinfo
            )
        }

        val objectMapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setTimeZone(TimeZone.getTimeZone("Europe/Oslo"))

        val arbeidsplassenStilling = arbeidsplassenKlient.hentStilling(stillingsId, somSystembruker)
        //log.info("Hentet stilling fra Arbeidsplassen $stillingsId")
        log.info("Stilling $stillingsId fra arbeidsplassen ${objectMapper.writeValueAsString(arbeidsplassenStilling)}")

        val stilling = stillingssokProxyClient.hentStilling(stillingsId, somSystembruker)
        log.info("Hentet stilling fra OpenSearch $stillingsId")

        return RekrutteringsbistandStilling(
            stillingsinfo = stillingsinfo,
            stilling = stilling.copyMedBeregnetTitle(stillingsinfo?.stillingskategori)
        )
    }

    fun opprettNyStilling(opprettDto: OpprettRekrutteringsbistandstillingDto): RekrutteringsbistandStilling {
        return opprettStilling(
            opprettStilling = opprettDto.stilling.toArbeidsplassenDto(title = "Ny stilling"),
            stillingskategori = opprettDto.kategori,
        )
    }

    private fun opprettStilling(opprettStilling: OpprettStillingDto, stillingskategori: Stillingskategori): RekrutteringsbistandStilling {
        val opprettetStillingArbeidsplassen = arbeidsplassenKlient.opprettStilling(opprettStilling)
        log.info("Opprettet stilling hos Arbeidsplassen med uuid: ${opprettetStillingArbeidsplassen.uuid}")
        val stillingsId = Stillingsid(opprettetStillingArbeidsplassen.uuid)

        // Opprett stilling i db med samme uuid som arbeidsplassen
        val opprettet = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo"))
        direktemeldtStillingRepository.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = stillingsId.verdi,
                innhold = opprettStilling.toDirektemeldtStillingInnhold(stillingsId, opprettet),
                opprettet = opprettet,
                opprettetAv = opprettStilling.createdBy,
                sistEndretAv = opprettStilling.updatedBy,
                sistEndret = opprettet,
                status = "INACTIVE",
                annonseId = null
            )
        )
        log.info("Opprettet stilling i databasen med uuid: ${opprettetStillingArbeidsplassen.uuid}")

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStillingArbeidsplassen)

        return RekrutteringsbistandStilling(
            stilling = opprettetStillingArbeidsplassen,
            stillingsinfo = stillingsinfo?.asStillingsinfoDto()
        ).also {
            kandidatlisteKlient.sendStillingOppdatert(it)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val eksisterendeRekrutteringsbistandStilling = hentRekrutteringsbistandStilling(stillingsId)
        val eksisterendeStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val kopi = eksisterendeStilling.toKopiertStilling(tokenUtils)

        return opprettStilling(
            kopi,
            kategoriMedDefault(eksisterendeRekrutteringsbistandStilling.stillingsinfo)
        )
    }


    fun kategoriMedDefault(stillingsInfo: StillingsinfoDto?) =
        if (stillingsInfo?.stillingskategori == null) Stillingskategori.STILLING else stillingsInfo.stillingskategori

    @Transactional
    fun oppdaterRekrutteringsbistandStilling(
        dto: OppdaterRekrutteringsbistandStillingDto,
        queryString: String?
    ): OppdaterRekrutteringsbistandStillingDto {
        loggEventuellOvertagelse(dto)

        val id = Stillingsid(dto.stilling.uuid)
        val eksisterendeStillingsinfo: Stillingsinfo? = stillingsinfoService.hentForStilling(id)

        val stilling = dto.stilling.copyMedBeregnetTitle(
            stillingskategori = eksisterendeStillingsinfo?.stillingskategori
        )

        // TODO: oppdater stilling i db
        direktemeldtStillingRepository.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = id.verdi,
                innhold = stilling.toDirektemeldtStillingInnhold(),
                opprettet = stilling.created.atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                status = stilling.status,
                annonseId = dto.stilling.id
            )
        )
        log.info("Oppdaterte stilling i databasen med uuid: ${dto.stilling.uuid}")

        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling, queryString)
        log.info("Oppdaterte stilling hos Arbeidsplassen med uuid: ${dto.stilling.uuid}")

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = oppdatertStilling,
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString()
        ).also {
            if (oppdatertStilling.source.equals("DIR", ignoreCase = false)) {
                kandidatlisteKlient.sendStillingOppdatert(
                    RekrutteringsbistandStilling(
                        stilling = oppdatertStilling,
                        stillingsinfo = eksisterendeStillingsinfo?.asStillingsinfoDto()
                    )
                )
            }
        }
    }

    private fun loggEventuellOvertagelse(dto: OppdaterRekrutteringsbistandStillingDto) {
        val gammelStilling = arbeidsplassenKlient.hentStilling(dto.stilling.uuid)
        val gammelEier = gammelStilling.administration?.navIdent
        val nyEier = dto.stilling.administration?.navIdent
        if (!nyEier.equals(gammelEier)) {
            AuditLogg.loggOvertattStilling(navIdent = nyEier ?: "", forrigeEier=gammelEier, stillingsid=gammelStilling.uuid)
        }
    }

    fun slettRekrutteringsbistandStilling(stillingsId: String): Stilling {
        kandidatlisteKlient.varsleOmSlettetStilling(Stillingsid(stillingsId))
        return arbeidsplassenKlient.slettStilling(stillingsId)
    }

    @Transactional
    fun lagreDirektemeldtStilling(stillingsId: String) {
        log.info("Hent stilling fra Arbeidsplassen og lagre til databasen uuid: $stillingsId")
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, true)

        val direktemeldtStillingInnhold = stilling.toDirektemeldtStillingInnhold()

        val direktemeldtStilling = DirektemeldtStilling(
            UUID.fromString(stillingsId),
            direktemeldtStillingInnhold,
            opprettet = stilling.created.atZone(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
            status = stilling.status,
            annonseId = null
        )
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)
    }

    fun hentDirektemeldtStilling(stillingsId: String): DirektemeldtStilling {
        return direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId) ?: throw RuntimeException("Fant ikke direktemeldt stilling $stillingsId")
    }

    fun hentAlleDirektemeldteStillinger(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentAlleDirektemeldteStillinger()
    }
}
