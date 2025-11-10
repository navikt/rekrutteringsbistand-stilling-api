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
import no.nav.rekrutteringsbistand.api.stilling.outbox.EventName
import no.nav.rekrutteringsbistand.api.stilling.outbox.StillingOutboxService
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
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
    val geografiService: GeografiService,
    val stillingOutboxService: StillingOutboxService,
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

    @Transactional
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

        if(stilling.medium == null) {
            stilling = stilling.copy(medium = "DIR")
        }

        // Opprett stilling i db med samme uuid som arbeidsplassen
        val opprettet = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("Europe/Oslo"))
        val uuid = UUID.randomUUID()
        direktemeldtStillingService.lagreDirektemeldtStilling(
            DirektemeldtStilling(
                stillingsId = uuid,
                innhold = stilling.toDirektemeldtStillingInnhold(uuid.toString()),
                opprettet = opprettet,
                opprettetAv = stilling.createdBy,
                sistEndretAv = stilling.updatedBy,
                sistEndret = opprettet,
                status = "INACTIVE",
                annonsenr = uuid.toString(),
                utløpsdato = if (stillingskategori == Stillingskategori.FORMIDLING) opprettet else LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS).atZone(ZoneId.of("Europe/Oslo")),
                publisert = opprettet,
                publisertAvAdmin = null,
                adminStatus = stilling.administration.status
            )
        )
        log.info("Opprettet stilling i databasen med uuid: ${uuid}")
        direktemeldtStillingService.settAnnonsenrFraDbId(uuid.toString())
        val direktemeldtStillingFraDb = direktemeldtStillingService.hentDirektemeldtStilling(uuid.toString())!!

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = Stillingsid(uuid),
            stillingskategori = stillingskategori,
            eierNavident = eierNavident,
            eierNavn = eierNavn,
            eierNavKontorEnhetId = eierNavKontorEnhetId,
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(Stillingsid(uuid))

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

    @Transactional
    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        log.info("Kopierer stilling med uuid: $stillingsId")
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
            if(eksisterendeStilling?.versjon != dto.stilling.versjon) {
                log.info("Stillingen ${dto.stilling.uuid} gir optimistic locking. Versjon fra frontend: ${dto.stilling.versjon}, eksisterende versjon: ${eksisterendeStilling?.versjon}")
               // throw ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Stillingen er allerede blitt oppdatert")
            }
        } else {
            throw IllegalArgumentException("Skal ikke kunne oppdatere stillinger som ikke er direktemeldt")
        }

        // Dette burde ikke skje lenger siden overta eierskap er flyttet ut
        loggEventuellOvertagelse(dto)

        val id = Stillingsid(dto.stilling.uuid)
        val eksisterendeStillingIDb = direktemeldtStillingService.hentDirektemeldtStilling(id.asString())
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
                versjon = dto.stilling.versjon ?: 1
            )
        )
        log.info("Oppdaterte stilling i databasen med uuid: ${dto.stilling.uuid}")
        val direktemeldtStillingFraDb = direktemeldtStillingService.hentDirektemeldtStilling(id.asString())!!

        if (PubliserteArbeidsplassenStillinger.erPublisertPåArbeidsplassenViaRestApi(direktemeldtStillingFraDb.stillingsId)) {
            log.info("Stillingen er publisert på arbeidsplassen, oppdaterer stilling der med uuid: ${dto.stilling.uuid}")
            // Hent stilling før den oppdateres, da det er en OptimisticLocking strategi på 'updated' feltet hos Arbeidsplassen
            val existerendeStilling = arbeidsplassenKlient.hentStilling(dto.stilling.uuid)
            arbeidsplassenKlient.oppdaterStilling(
                stilling.toArbeidsplassenDto(existerendeStilling.id).copy(updated = existerendeStilling.updated,), queryString
            )
            log.info("Oppdaterte stilling hos Arbeidsplassen med uuid: ${dto.stilling.uuid}")
        } else {
            // Sjekk om stillingen skal sendes til arbeidsplassen
            if (eksisterendeStillingsinfo?.stillingskategori != Stillingskategori.FORMIDLING && (dto.stilling.privacy == "SHOW_ALL"
                        || (eksisterendeStillingIDb?.innhold?.privacy == "SHOW_ALL" && dto.stilling.privacy == "INTERNAL_NOT_SHOWN"))) {
                stillingOutboxService.lagreMeldingIOutbox(
                    stillingsId = id.verdi,
                    eventName = EventName.PUBLISER_ELLER_AVPUBLISER_TIL_ARBEIDSPLASSEN
                )
            }
        }

        val oppdatertStillingsinfo: Stillingsinfo? = stillingsinfoService.hentStillingsinfo(id)

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = direktemeldtStillingFraDb.toStilling(),
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString(),
            stillingsinfo = oppdatertStillingsinfo?.asStillingsinfoDto(),
        ).also {
            if (direktemeldtStillingFraDb.innhold.source.equals("DIR", ignoreCase = false)) {
                val kandidatListeDto = KandidatlisteDto(
                    stillingsinfo = eksisterendeStillingsinfo?.asStillingsinfoDto(),
                    stilling = KandidatlisteStillingDto(direktemeldtStillingFraDb)
                )
                kandidatlisteKlient.sendStillingOppdatert(kandidatListeDto)
            }
        }
    }

    private fun loggEventuellOvertagelse(dto: OppdaterRekrutteringsbistandStillingDto) {
        val gammelStilling = direktemeldtStillingService.hentDirektemeldtStilling(dto.stilling.uuid)
        val gammelEier = gammelStilling?.innhold?.administration?.navIdent
        val nyEier = dto.stilling.administration?.navIdent
        if (!nyEier.equals(gammelEier)) {
            AuditLogg.loggOvertattStilling(navIdent = nyEier ?: "", forrigeEier=gammelEier, stillingsid=gammelStilling?.stillingsId.toString())
        }
    }
    @Transactional
    fun slettRekrutteringsbistandStilling(stillingsId: String): FrontendStilling {
        try {
            UUID.fromString(stillingsId)
        } catch (_: IllegalArgumentException) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Ugyldig stillingsId. Må være en gyldig UUID."
            )
        }

        kandidatlisteKlient.varsleOmSlettetStilling(Stillingsid(stillingsId))

        val direktemeldtStilling = direktemeldtStillingService.hentDirektemeldtStilling(stillingsId)
            ?: throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Stilling med id $stillingsId finnes ikke i databasen."
            )

        val slettetStilling = direktemeldtStilling.copy(
            status = "DELETED",
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        direktemeldtStillingService.lagreDirektemeldtStilling(slettetStilling)

        if (PubliserteArbeidsplassenStillinger.erPublisertPåArbeidsplassenViaRestApi(direktemeldtStilling.stillingsId)) {
            arbeidsplassenKlient.slettStilling(stillingsId)
        }

        return slettetStilling.toStilling()
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
