package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.AuditLogg
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*


@Service
class StillingService(
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient,
    val direktemeldtStillingRepository: DirektemeldtStillingRepository
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

        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, somSystembruker)
        log.info("Hentet stilling fra Arbeidsplassen $stillingsId")
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
        val arbeidsplassenStilling = arbeidsplassenKlient.hentStilling(stillingsId, true)

        direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)?.let { dbStilling ->
            logDiff(dbStilling, arbeidsplassenStilling, stillingsId)
        }

        val direktemeldtStillingInnhold = arbeidsplassenStilling.toDirektemeldtStillingInnhold()

        val direktemeldtStilling = DirektemeldtStilling(
            UUID.fromString(stillingsId),
            direktemeldtStillingInnhold,
            opprettet = arbeidsplassenStilling.created.atZone(ZoneId.of("Europe/Oslo")),
            opprettetAv = arbeidsplassenStilling.createdBy,
            sistEndretAv = arbeidsplassenStilling.updatedBy,
            sistEndret = arbeidsplassenStilling.updated.atZone(ZoneId.of("Europe/Oslo")),
            status = arbeidsplassenStilling.status,
            annonseId = null
        )
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)
    }

    private fun logDiff(dbStilling: DirektemeldtStilling, arbeidsplassenStilling: Stilling, stillingsId: String) {
//        loggHvisDiff(dbStilling.annonseId, arbeidsplassenStilling.id, "annonseId", stillingsId) // todo: denne gir alltid diff nå - finn en løsning for å beholde id'en
        loggHvisDiff(dbStilling.stillingsId.toString(), arbeidsplassenStilling.uuid, "uuid", stillingsId)
        loggHvisDiff(dbStilling.status, arbeidsplassenStilling.status, "status", stillingsId)
        loggHvisDiff(dbStilling.sistEndretAv, arbeidsplassenStilling.updatedBy, "sistEndretAv", stillingsId)
        loggHvisDiff(dbStilling.opprettetAv, arbeidsplassenStilling.createdBy, "opprettetAv", stillingsId)
        loggHvisDiff(dbStilling.innhold.source, arbeidsplassenStilling.source, "source", stillingsId)
        loggHvisDiff(dbStilling.innhold.title, arbeidsplassenStilling.title, "title", stillingsId)
        loggHvisDiff(dbStilling.innhold.medium, arbeidsplassenStilling.medium, "medium", stillingsId)
        loggHvisDiff(dbStilling.innhold.privacy, arbeidsplassenStilling.privacy, "privacy", stillingsId)
        loggHvisDiff(dbStilling.innhold.reference, arbeidsplassenStilling.reference, "reference", stillingsId)
        loggHvisDiff(dbStilling.innhold.expires?.toLocalDateTime(), arbeidsplassenStilling.expires, "expires", stillingsId)
        loggHvisDiffMerEnn5min(dbStilling.innhold.published?.toLocalDateTime(), arbeidsplassenStilling.published, "published", stillingsId)
        loggHvisDiff(dbStilling.innhold.locationList.size, arbeidsplassenStilling.locationList.size, "locationList.size", stillingsId)
        if (dbStilling.innhold.locationList.isNotEmpty() && arbeidsplassenStilling.locationList.isNotEmpty()) {
            loggHvisDiff(dbStilling.innhold.locationList[0], arbeidsplassenStilling.locationList[0], "locationList.size[0]", stillingsId)
        }
        loggHvisDiff(dbStilling.innhold.contactList.size, arbeidsplassenStilling.contactList.size, "locationList.size", stillingsId)
        loggHvisDiff(dbStilling.innhold.mediaList.size, arbeidsplassenStilling.mediaList.size, "locationList.size", stillingsId)
        loggHvisDiff(dbStilling.innhold.administration?.status, arbeidsplassenStilling.administration?.status, "administration.status", stillingsId)
        loggHvisDiff(dbStilling.innhold.administration?.remarks, arbeidsplassenStilling.administration?.remarks, "administration.remarks", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.name, arbeidsplassenStilling.employer?.name, "employer.name", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.orgnr, arbeidsplassenStilling.employer?.orgnr, "employer.orgnr", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.employees, arbeidsplassenStilling.employer?.employees, "employer.employees", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "extent", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "workday", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "positioncount", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "engagementtype", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "starttime", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "employerdescription", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "remote", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "employer", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "sector", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "workhours", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "applicationdue", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "jobtitle", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "jobarrangement", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "adtext", stillingsId)
    }

    fun loggHvisDiff(db: Any?, arbeidsplassen: Any?, propertyName: String, stillingsId: String) {
        if (db != arbeidsplassen) {
            log.info("Diff i '$propertyName' (db: $db / arbeidsplassen: $arbeidsplassen) $stillingsId")
        }
    }

    fun loggHvisDiff(dbProperties: Map<String, String>, arbeidsplassenProperties: Map<String, String>, propertyName: String, stillingsId: String) {
        if (dbProperties[propertyName] != arbeidsplassenProperties[propertyName]) {
            log.info("Diff i 'properties.$propertyName' (db: ${dbProperties[propertyName]} / arbeidsplassen: ${arbeidsplassenProperties[propertyName]}) $stillingsId")
        }
    }

    fun loggHvisDiffMerEnn5min(db: LocalDateTime?, arbeidsplassen: LocalDateTime?, propertyName: String, stillingsId: String) {
        if (db != arbeidsplassen) {
            if (db != null && arbeidsplassen != null) {
                val minutesBetween = Duration.between(db, arbeidsplassen).toMinutes()
                if (minutesBetween > 5) {
                    log.info("Diff i '$propertyName' of more than 5 minutes (db: $db / arbeidsplassen: $arbeidsplassen) $stillingsId")
                }
            } else {
                log.info("Diff i '$propertyName' (db: $db / arbeidsplassen: $arbeidsplassen) $stillingsId")
            }
        }
    }


    fun hentDirektemeldtStilling(stillingsId: String): DirektemeldtStilling {
        return direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId) ?: throw RuntimeException("Fant ikke direktemeldt stilling $stillingsId")
    }

    fun hentAlleDirektemeldteStillinger(): List<DirektemeldtStilling> {
        return direktemeldtStillingRepository.hentAlleDirektemeldteStillinger()
    }
}
