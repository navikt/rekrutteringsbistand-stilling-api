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
import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.Stilling.Companion.DEFAULT_EXPIRY_DAYS
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
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
    val stillingssokProxyClient: StillingssokProxyClient,
    val geografiService: GeografiService
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
        val populertGeografi = populerMedManglendeFylke(opprettStilling.employer?.location)
        var stilling = opprettStilling.copy(employer = opprettStilling.employer?.copy(location = populertGeografi))

        val opprettetStillingArbeidsplassen = arbeidsplassenKlient.opprettStilling(stilling)
        log.info("Opprettet stilling hos Arbeidsplassen med uuid: ${opprettetStillingArbeidsplassen.uuid}")
        val stillingsId = Stillingsid(opprettetStillingArbeidsplassen.uuid)

        if(stilling.medium == null) {
            stilling = stilling.copy(medium = "DIR")
        }

        // Opprett stilling i db med samme uuid som arbeidsplassen
        val opprettet = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo"))
        direktemeldtStillingRepository.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = stillingsId.verdi,
                innhold = stilling.toDirektemeldtStillingInnhold(stillingsId),
                opprettet = opprettet,
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = opprettet,
                status = "INACTIVE",
                annonseId = null,
                utløpsdato = if (stillingskategori == Stillingskategori.FORMIDLING) opprettet else LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS).atZone(ZoneId.of("Europe/Oslo")),
                publisert = opprettet,
                publisertAvAdmin = null,
                adminStatus = stilling.administration.status
            )
        )
        log.info("Opprettet stilling i databasen med uuid: ${opprettetStillingArbeidsplassen.uuid}")
        val direktemeldtStillingFraDb = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)!!

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStillingArbeidsplassen)

        return RekrutteringsbistandStilling(
            stilling = direktemeldtStillingFraDb.toStilling(),
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
        log.info("Oppdaterer stilling med uuid: ${dto.stilling.uuid}")
        loggEventuellOvertagelse(dto)

        val id = Stillingsid(dto.stilling.uuid)
        val eksisterendeStillingsinfo: Stillingsinfo? = stillingsinfoService.hentForStilling(id)

        var publishedByAdmin: String? = dto.stilling.publishedByAdmin
        if(dto.stilling.firstPublished != null && dto.stilling.firstPublished && publishedByAdmin == null) {
            publishedByAdmin = LocalDateTime.now(ZoneId.of("Europe/Oslo")).toString()
        }

        val stilling = dto.stilling.copyMedBeregnetTitle(
            stillingskategori = eksisterendeStillingsinfo?.stillingskategori
        ).copy(updated = LocalDateTime.now(ZoneId.of("Europe/Oslo")), publishedByAdmin = publishedByAdmin)

        direktemeldtStillingRepository.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = id.verdi,
                innhold = stilling.toDirektemeldtStillingInnhold(),
                opprettet = stilling.created.atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                status = stilling.status,
                annonseId = dto.stilling.id,
                utløpsdato = dto.stilling.hentExpiresMedDefaultVerdiOmIkkeOppgitt(),
                publisert = dto.stilling.published?.atZone(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = publishedByAdmin,
                adminStatus = dto.stilling.administration?.status
            )
        )
        log.info("Oppdaterte stilling i databasen med uuid: ${dto.stilling.uuid}")
        val direktemeldtStillingFraDb = direktemeldtStillingRepository.hentDirektemeldtStilling(id.asString())!!

        // Hent stilling før den oppdateres, da det er en OptimisticLocking strategi på 'updated' feltet hos Arbeidsplassen
        val existerendeStilling = arbeidsplassenKlient.hentStilling(dto.stilling.uuid)
        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling.copy(updated = existerendeStilling.updated), queryString)
        log.info("Oppdaterte stilling hos Arbeidsplassen med uuid: ${dto.stilling.uuid}")

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = direktemeldtStillingFraDb.toStilling(),
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

        direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)?.let { dbStilling ->
            direktemeldtStillingRepository.lagreDirektemeldtStilling(dbStilling.copy(
                status = "DELETED",
                sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))))
        }

        return arbeidsplassenKlient.slettStilling(stillingsId)
    }

    @Transactional
    fun lagreDirektemeldtStilling(stillingsId: String) {
        log.info("Hent stilling fra Arbeidsplassen og lagre til databasen uuid: $stillingsId")
        val arbeidsplassenStilling = arbeidsplassenKlient.hentStilling(stillingsId, true)

        direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)?.let { dbStilling ->
            logDiff(dbStilling, arbeidsplassenStilling, stillingsId)
        }
        val finnesStilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)
        if(finnesStilling == null) {
            log.warn("Stilling $stillingsId finnes ikke i databasen")
            val direktemeldtStillingInnhold = arbeidsplassenStilling.toDirektemeldtStillingInnhold()
            val direktemeldtStilling = DirektemeldtStilling(
                UUID.fromString(stillingsId),
                direktemeldtStillingInnhold,
                opprettet = arbeidsplassenStilling.created.atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = arbeidsplassenStilling.createdBy,
                sistEndretAv = arbeidsplassenStilling.updatedBy,
                sistEndret = arbeidsplassenStilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                status = arbeidsplassenStilling.status,
                annonseId = null,
                utløpsdato = arbeidsplassenStilling.hentExpiresMedDefaultVerdiOmIkkeOppgitt(),
                publisert = arbeidsplassenStilling.published?.atZone(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = arbeidsplassenStilling.publishedByAdmin,
                adminStatus = arbeidsplassenStilling.administration?.status
            )
            direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)
        }
    }

    private fun logDiff(dbStilling: DirektemeldtStilling, arbeidsplassenStilling: Stilling, stillingsId: String) {
        loggHvisDiff(dbStilling.stillingsId.toString(), arbeidsplassenStilling.uuid, "uuid", stillingsId)
        loggHvisDiff(dbStilling.status, arbeidsplassenStilling.status, "status", stillingsId)
        loggHvisDiff(dbStilling.opprettetAv, arbeidsplassenStilling.createdBy, "opprettetAv", stillingsId)
        loggHvisDiff(dbStilling.innhold.source, arbeidsplassenStilling.source, "source", stillingsId)
        loggHvisDiff(dbStilling.innhold.title, arbeidsplassenStilling.title, "title", stillingsId)
        loggHvisDiff(dbStilling.innhold.medium, arbeidsplassenStilling.medium, "medium", stillingsId)
        loggHvisDiff(dbStilling.innhold.privacy, arbeidsplassenStilling.privacy, "privacy", stillingsId)
        loggHvisDiff(dbStilling.innhold.reference, arbeidsplassenStilling.reference, "reference", stillingsId)
        loggHvisDiffMerEnn5min(dbStilling.utløpsdato?.toLocalDateTime(), arbeidsplassenStilling.expires, "expires", stillingsId)
        loggHvisDiffMerEnn5min(dbStilling.publisert?.toLocalDateTime(), arbeidsplassenStilling.published, "published", stillingsId)
        loggHvisDiff(dbStilling.innhold.locationList.size, arbeidsplassenStilling.locationList.size, "locationList.size", stillingsId)
        if (dbStilling.innhold.locationList.isNotEmpty() && arbeidsplassenStilling.locationList.isNotEmpty()) {
            loggHvisDiff(dbStilling.innhold.locationList[0], arbeidsplassenStilling.locationList[0], "locationList.size[0]", stillingsId)
        }
        loggHvisDiff(dbStilling.innhold.businessName, arbeidsplassenStilling.businessName, "businessName", stillingsId)
        loggHvisDiff(dbStilling.innhold.firstPublished, arbeidsplassenStilling.firstPublished, "firstPublished", stillingsId)
        loggHvisDiff(dbStilling.innhold.contactList.size, arbeidsplassenStilling.contactList.size, "contactList.size", stillingsId)
        loggHvisDiff(dbStilling.innhold.mediaList.size, arbeidsplassenStilling.mediaList.size, "mediaList.size", stillingsId)
        loggHvisDiff(dbStilling.innhold.administration?.status, arbeidsplassenStilling.administration?.status, "administration.status", stillingsId)
        loggHvisDiff(dbStilling.innhold.administration?.remarks, arbeidsplassenStilling.administration?.remarks, "administration.remarks", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.name, arbeidsplassenStilling.employer?.name, "employer.name", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.orgnr, arbeidsplassenStilling.employer?.orgnr, "employer.orgnr", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.employees, arbeidsplassenStilling.employer?.employees, "employer.employees", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.location, arbeidsplassenStilling.employer?.location, "employer.location", stillingsId)
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
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "tags", stillingsId)
        loggHvisDiff(dbStilling.innhold.properties, arbeidsplassenStilling.properties, "jobpercentage", stillingsId)
    }

    fun loggHvisDiff(db: Any?, arbeidsplassen: Any?, propertyName: String, stillingsId: String) {
        if (db != arbeidsplassen) {
            log.info("Diff i '$propertyName' (db: $db / arbeidsplassen: $arbeidsplassen) $stillingsId")
        }
    }

    fun loggHvisDiff(dbProperties: Map<String, String>, arbeidsplassenProperties: Map<String, String>, propertyName: String, stillingsId: String) {
        if (dbProperties[propertyName] == null && arbeidsplassenProperties[propertyName] == "[]") {
            return // Ingen diff
        }
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

    fun populerMedManglendeFylke(geografi: Geografi?) : Geografi? {
        if(geografi == null) {
            return null
        }

        if(geografi.county.isNullOrBlank() && !geografi.municipalCode.isNullOrBlank()) {
            val fylke = geografiService.finnFylke(geografi.municipalCode)
            return geografi.copy(county = fylke)
        }
        return geografi
    }
}
