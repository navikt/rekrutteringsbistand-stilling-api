package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.AuditLogg
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


@Service
class StillingService(
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient,
    val internStillingRepository: InternStillingRepository
) {
    fun hentRekrutteringsbistandStilling(
        stillingsId: String,
        somSystembruker: Boolean = false
    ): RekrutteringsbistandStilling {
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, somSystembruker)
        val stillingsinfo = stillingsinfoService
            .hentStillingsinfo(stilling)
            .map { it.asStillingsinfoDto() }
            .getOrElse { null }

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
        val opprettetInternStilling = lagStilling(opprettStilling)
        val opprettetStilling = arbeidsplassenKlient.opprettStilling(opprettStilling)
        val stillingsId = Stillingsid(opprettetStilling.uuid)

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori
        )

        // Lagre stillingen i intern_stilling
        val internStilling = opprettInternStilling(opprettetInternStilling, opprettetInternStilling.uuid)
        internStillingRepository.lagreInternStilling(internStilling)

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStilling)

        return RekrutteringsbistandStilling(
            stilling = opprettetStilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.orNull()
        ).also {
            kandidatlisteKlient.sendStillingOppdatert(it)
        }
    }

    private fun lagStilling(opprettStilling: OpprettStillingDto) : NyInternStilling {
        val uuid = UUID.randomUUID().toString()
        val opprettetTidspunkt = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).toLocalDateTime()
        val opprettetInternStilling = NyInternStilling(
            uuid = uuid,
            created = opprettetTidspunkt,
            createdBy = opprettStilling.createdBy,
            updated = opprettetTidspunkt,
            updatedBy = opprettStilling.updatedBy,
            title = "Ny stilling",
            status = "INACTIVE",
            administration = Administration(
                status = opprettStilling.administration.status,
                navIdent = opprettStilling.administration.navIdent,
                reportee = opprettStilling.administration.reportee,
                comments = null,
                remarks = emptyList(),
                id = null // TODO må fjernes etterhvert
            ),
            mediaList = emptyList(),
            contactList = emptyList(),
            privacy = opprettStilling.privacy,
            source = opprettStilling.source,
            medium = opprettStilling.source,  // medium === source
            reference = uuid,
            published = opprettetTidspunkt,
            expires = opprettetTidspunkt,
            employer = opprettStilling.employer,
            location = null,
            locationList = emptyList(),
            categoryList = emptyList(),
            properties = emptyMap(),
            publishedByAdmin = null,
            businessName = opprettStilling.employer?.name,
            firstPublished = false,
            deactivatedByExpiry = false,
            activationOnPublishingDate = false,
        )

        return opprettetInternStilling
    }

    private fun opprettInternStilling(opprettStilling: NyInternStilling, stillingsId: String) : InternStilling {
        val internStilling = InternStilling(
            UUID.fromString(stillingsId),
            opprettStilling,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = opprettStilling.createdBy,
            sistEndretAv = opprettStilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        return internStilling
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
        val eksisterendeStillingsinfo: Stillingsinfo? =
            stillingsinfoService.hentForStilling(id).orNull()

        val stilling = dto.stilling.copyMedBeregnetTitle(
            stillingskategori = eksisterendeStillingsinfo?.stillingskategori
        )

        val internStilling = opprettInternStilling(dto.stilling.toNyInternStilling(), id.asString())
        internStillingRepository.lagreInternStilling(internStilling)

        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling, queryString)

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

    fun lagreInternStilling(stillingsId: String) {
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, true)

        val nyInternStilling = stilling.toNyInternStilling()

        val internStilling = InternStilling(
            UUID.fromString(stillingsId),
            nyInternStilling,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        internStillingRepository.lagreInternStilling(internStilling)
    }
}
