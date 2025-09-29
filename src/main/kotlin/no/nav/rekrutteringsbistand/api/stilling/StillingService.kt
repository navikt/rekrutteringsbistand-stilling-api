package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.AuditLogg
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.geografi.GeografiService
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteDto
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteStillingDto
import no.nav.rekrutteringsbistand.api.opensearch.StillingssokProxyClient
import no.nav.rekrutteringsbistand.api.stilling.FrontendStilling.Companion.DEFAULT_EXPIRY_DAYS
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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
    val direktemeldtStillingService: DirektemeldtStillingService,
    val stillingssokProxyClient: StillingssokProxyClient,
    val geografiService: GeografiService
) {
    fun hentRekrutteringsbistandStilling(
        stillingsId: String,
        somSystembruker: Boolean = false
    ): RekrutteringsbistandStilling {
        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(stillingsId))?.asStillingsinfoDto()

        direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)?.let { direktemeldtStilling ->
            log.info("Hentet stilling fra databasen $stillingsId")
            return RekrutteringsbistandStilling(
                stilling = direktemeldtStilling.toStilling().copyMedBeregnetTitle(stillingsinfo?.stillingskategori),
                stillingsinfo = stillingsinfo
            )
        }

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
            eierNavident = opprettDto.eierNavident,
            eierNavn = opprettDto.eierNavn,
            eierNavKontorEnhetId = opprettDto.eierNavKontorEnhetId,
        )
    }

    private fun opprettStilling(
        opprettStilling: OpprettStillingDto,
        stillingskategori: Stillingskategori,
        eierNavident: String?,
        eierNavn: String?,
        eierNavKontorEnhetId: String?,
    ): RekrutteringsbistandStilling {
        val populertGeografi = populerGeografi(opprettStilling.employer?.location)
        var stilling = opprettStilling.copy(employer = opprettStilling.employer?.copy(location = populertGeografi))

        val opprettetStillingArbeidsplassen = arbeidsplassenKlient.opprettStilling(stilling)
        log.info("Opprettet stilling hos Arbeidsplassen med uuid: ${opprettetStillingArbeidsplassen.uuid}")
        val stillingsId = Stillingsid(opprettetStillingArbeidsplassen.uuid)

        if(stilling.medium == null) {
            stilling = stilling.copy(medium = "DIR")
        }

        // Opprett stilling i db med samme uuid som arbeidsplassen
        val opprettet = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo"))
        direktemeldtStillingService.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = stillingsId.verdi,
                innhold = stilling.toDirektemeldtStillingInnhold(stillingsId),
                opprettet = opprettet,
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = opprettet,
                status = "INACTIVE",
                annonsenr = stillingsId.verdi.toString(),
                utløpsdato = if (stillingskategori == Stillingskategori.FORMIDLING) opprettet else LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS).atZone(ZoneId.of("Europe/Oslo")),
                publisert = opprettet,
                publisertAvAdmin = null,
                adminStatus = stilling.administration.status
            )
        )
        log.info("Opprettet stilling i databasen med uuid: ${opprettetStillingArbeidsplassen.uuid}")
        direktemeldtStillingService.settAnnonsenrFraDbId(stillingsId.asString())
        val direktemeldtStillingFraDb = direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)!!

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori,
            eierNavident = eierNavident,
            eierNavn = eierNavn,
            eierNavKontorEnhetId = eierNavKontorEnhetId,
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(opprettetStillingArbeidsplassen.uuid))

        return RekrutteringsbistandStilling(
            stilling = direktemeldtStillingFraDb.toStilling(),
            stillingsinfo = stillingsinfo?.asStillingsinfoDto()
        ).also {
            val kandidatListeDto = KandidatlisteDto(
                stillingsinfo = stillingsinfo?.asStillingsinfoDto(),
                stilling = KandidatlisteStillingDto(direktemeldtStillingFraDb)
            )
            kandidatlisteKlient.sendStillingOppdatert(kandidatListeDto)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val eksisterendeRekrutteringsbistandStilling = hentRekrutteringsbistandStilling(stillingsId)
        val eksisterendeStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val kopi = eksisterendeStilling.toKopiertStilling(tokenUtils)

        return opprettStilling(
            opprettStilling = kopi,
            stillingskategori = kategoriMedDefault(eksisterendeRekrutteringsbistandStilling.stillingsinfo),
            eierNavKontorEnhetId = eksisterendeRekrutteringsbistandStilling.stillingsinfo?.eierNavKontorEnhetId,
            eierNavident = eksisterendeRekrutteringsbistandStilling.stillingsinfo?.eierNavident,
            eierNavn = eksisterendeRekrutteringsbistandStilling.stillingsinfo?.eierNavn,
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

        if(dto.stilling.source == "DIR") {
            val eksisterendeStilling = direktemeldtStillingService.hentDirektemeldtStilling(dto.stilling.uuid)
            log.info("Versjon på stilling ${dto.stilling.uuid}. Oppdatert: ${dto.stilling.versjon}, tidligere: ${eksisterendeStilling?.versjon}")
            if( eksisterendeStilling?.versjon != dto.stilling.versjon) {
                log.warn("Stillinger er allerede blitt oppdatert og skaper optimistic locking")
                throw ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Stillingen er allerede blitt oppdatert")
            }
        } else {
            throw IllegalArgumentException("Skal ikke kunne oppdatere stillinger som ikke er direktemeldt")
        }

        // Dette burde ikke skje lenger siden overta eierskap er flyttet ut
        loggEventuellOvertagelse(dto)

        val id = Stillingsid(dto.stilling.uuid)
        val eksisterendeStillingsinfo: Stillingsinfo? = stillingsinfoService.hentStillingsinfo(id)

        // Dette vil hjelpe til med å fylle ut navkontor for stillinger som ikke allerede har det satt hvis de blir oppdatert
        if (eksisterendeStillingsinfo != null && dto.stillingsinfo?.eierNavKontorEnhetId != null) {
            stillingsinfoService.endreNavKontor(
                stillingsinfoId = eksisterendeStillingsinfo.stillingsinfoid,
                navKontorEnhetId = dto.stillingsinfo.eierNavKontorEnhetId,
            )
        } else if (eksisterendeStillingsinfo == null) {
            log.info("Fant ikke stillingsinfo for stilling med id ved en oppdatering: ${dto.stilling.uuid}")
        }

        var publishedByAdmin: String? = dto.stilling.publishedByAdmin
        if(dto.stilling.firstPublished != null && dto.stilling.firstPublished && publishedByAdmin == null) {
            publishedByAdmin = LocalDateTime.now(ZoneId.of("Europe/Oslo")).toString()
        }

        val populertLocationList = populerLocationList(dto.stilling.locationList)
        val stilling = dto.stilling.copyMedBeregnetTitle(
            stillingskategori = eksisterendeStillingsinfo?.stillingskategori
        ).copy(updated = LocalDateTime.now(ZoneId.of("Europe/Oslo")), publishedByAdmin = publishedByAdmin, locationList = populertLocationList)

        direktemeldtStillingService.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = id.verdi,
                innhold = stilling.toDirektemeldtStillingInnhold(),
                opprettet = stilling.created.atZone(ZoneId.of("Europe/Oslo")),
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
                status = stilling.status,
                annonsenr = dto.stilling.annonsenr,
                utløpsdato = dto.stilling.hentExpiresMedDefaultVerdiOmIkkeOppgitt(),
                publisert = dto.stilling.published?.atZone(ZoneId.of("Europe/Oslo")) ?: ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = publishedByAdmin,
                adminStatus = dto.stilling.administration?.status,
            )
        )
        log.info("Oppdaterte stilling i databasen med uuid: ${dto.stilling.uuid}")
        val direktemeldtStillingFraDb = direktemeldtStillingService.hentDirektemeldtStilling(id.asString())!!

        // Hent stilling før den oppdateres, da det er en OptimisticLocking strategi på 'updated' feltet hos Arbeidsplassen
        val existerendeStilling = arbeidsplassenKlient.hentStilling(dto.stilling.uuid)
        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling.toArbeidsplassenDto(existerendeStilling.id).copy(
            id = existerendeStilling.id,
            updated = existerendeStilling.updated,
        ), queryString)
        val oppdatertStillingsinfo: Stillingsinfo? = stillingsinfoService.hentStillingsinfo(id)
        log.info("Oppdaterte stilling hos Arbeidsplassen med uuid: ${dto.stilling.uuid}")

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = direktemeldtStillingFraDb.toStilling(),
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString(),
            stillingsinfo = oppdatertStillingsinfo?.asStillingsinfoDto(),
        ).also {
            if (oppdatertStilling.source.equals("DIR", ignoreCase = false)) {
                val kandidatListeDto = KandidatlisteDto(
                    stillingsinfo = eksisterendeStillingsinfo?.asStillingsinfoDto(),
                    stilling = KandidatlisteStillingDto(direktemeldtStillingFraDb)
                )
                kandidatlisteKlient.sendStillingOppdatert(kandidatListeDto)
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

    fun slettRekrutteringsbistandStilling(stillingsId: String): FrontendStilling {
        try {
            UUID.fromString(stillingsId)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Ugyldig stillingsId. Må være en gyldig UUID."
            )
        }

        kandidatlisteKlient.varsleOmSlettetStilling(Stillingsid(stillingsId))

        direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)?.let { dbStilling ->
            direktemeldtStillingService.lagreDirektemeldtStilling(dbStilling.copy(
                status = "DELETED",
                sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))))
        }

        return arbeidsplassenKlient.slettStilling(stillingsId)
    }

    @Transactional
    fun lagreDirektemeldtStilling(stillingsId: String) {
        log.info("Hent stilling fra Arbeidsplassen og lagre til databasen uuid: $stillingsId")
        val arbeidsplassenStillingDto = arbeidsplassenKlient.hentStilling(stillingsId, true)
        val arbeidsplassenStillingId = arbeidsplassenStillingDto.id
        val arbeidsplassenStilling = arbeidsplassenStillingDto.toStilling()

        direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)?.let { dbStilling ->
            logDiff(dbStilling, arbeidsplassenStilling, stillingsId)
        }
        val finnesStilling = direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)
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
                annonsenr = arbeidsplassenStillingId.toString(),
                utløpsdato = arbeidsplassenStilling.hentExpiresMedDefaultVerdiOmIkkeOppgitt(),
                publisert = arbeidsplassenStilling.published?.atZone(ZoneId.of("Europe/Oslo")),
                publisertAvAdmin = arbeidsplassenStilling.publishedByAdmin,
                adminStatus = arbeidsplassenStilling.administration?.status
            )
            direktemeldtStillingService.lagreDirektemeldtStilling(direktemeldtStilling)
        }
    }

    private fun logDiff(dbStilling: DirektemeldtStilling, arbeidsplassenStilling: FrontendStilling, stillingsId: String) {
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
        loggHvisDiff(dbStilling.adminStatus, arbeidsplassenStilling.administration?.status, "administration.status", stillingsId)
        loggHvisDiff(dbStilling.innhold.administration?.remarks, arbeidsplassenStilling.administration?.remarks, "administration.remarks", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.name, arbeidsplassenStilling.employer?.name, "employer.name", stillingsId)
        loggHvisDiff(dbStilling.innhold.employer?.orgnr, arbeidsplassenStilling.employer?.orgnr, "employer.orgnr", stillingsId)
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
        return direktemeldtStillingService.hentDirektemeldtStilling(stillingsId) ?: throw RuntimeException("Fant ikke direktemeldt stilling $stillingsId")
    }

    fun hentAlleStillingsIder(): List<UUID> {
        return direktemeldtStillingService.hentAlleStillingsIder()
    }

    fun populerLocationList(locationList: List<Geografi>): List<Geografi> {
        return locationList.mapNotNull { geografi -> populerGeografi(geografi) }
    }

    fun populerGeografi(geografi: Geografi?): Geografi? {
        if(geografi == null) {
            return null
        }

        if (!geografi.postalCode.isNullOrBlank()) {
            val postdata = geografiService.finnPostdata(geografi.postalCode)
            if (postdata != null) {
                return geografi.copy(
                    municipal = postdata.kommune.navn,
                    county = postdata.fylke.navn,
                    municipalCode = postdata.kommune.kommunenummer,
                    country = "NORGE",
                    city = postdata.by
                )
            }
        }

        val postDataFraKommune = geografiService.finnPostdataFraKommune(geografi.municipalCode, geografi.municipal)
        if(postDataFraKommune != null) {
            return geografi.copy(
                municipalCode = postDataFraKommune.kommune.kommunenummer,
                municipal = postDataFraKommune.kommune.navn,
                county = postDataFraKommune.fylke.navn,
                country = "NORGE",
            )
        }

        if(!geografi.county.isNullOrBlank()) {
            val fylke = geografiService.finnFylke(geografi.county)
            if (fylke != null) {
                return geografi.copy(
                    county = fylke,
                    country = "NORGE"
                )
            }
        }
        return geografi
    }
}
