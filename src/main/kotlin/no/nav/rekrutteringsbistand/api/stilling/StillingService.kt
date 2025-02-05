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
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, somSystembruker)
        val stillingsinfo = stillingsinfoService
            .hentStillingsinfo(stilling)
            ?.asStillingsinfoDto()

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
        log.info("Stilling som blir mottatt ved opprettelse i frontend: $opprettStilling")
        // berik stillingen med det den får fra pam-ad

        //val uuid = UUID.randomUUID()
        // send denne inn i opprettDirektemeldtStilling

        val opprettetStilling = arbeidsplassenKlient.opprettStilling(opprettStilling)

        val uuid = opprettetStilling.uuid
        log.info("Stilling som er opprettet i pam-ad: $opprettetStilling")
        val stillingsId = Stillingsid(uuid)

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori
        )

        // Lagrer annonsen i databasen ved opprettelse
        val direktemeldtStilling = opprettDirektemeldtStilling(opprettStilling, UUID.fromString(uuid))

        val stilling = direktemeldtStilling.toStilling()

        log.info("Direktemeldt stilling som blir lagret i databasen ved opprettelse: $direktemeldtStilling")
        direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)

        val stillingsinfo = stillingsinfoService.hentForStilling(stillingsId)

        return RekrutteringsbistandStilling(
            stilling = stilling,
            stillingsinfo = stillingsinfo?.asStillingsinfoDto()
        ).also {
            kandidatlisteKlient.sendStillingOppdatert(it)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val direktemeldtStilling = direktemeldtStillingRepository.hentDirektemeldtStilling(stillingsId)
        val direktemeldtStillingKopi = direktemeldtStilling.toKopiertStilling(tokenUtils)
        val direktemeldtStillingInfo = stillingsinfoService
            .hentForStilling(Stillingsid(stillingsId))
            ?.asStillingsinfoDto()

        val eksisterendeRekrutteringsbistandStilling = hentRekrutteringsbistandStilling(stillingsId)
        val eksisterendeStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val kopi = eksisterendeStilling.toKopiertStilling(tokenUtils)

        log.info("Kopi som ble opprettet før: $kopi")
        log.info("Kopi som ble opprettet nå fra databasen: $direktemeldtStillingKopi")

        return opprettStilling(
            direktemeldtStillingKopi,
            kategoriMedDefault(direktemeldtStillingInfo)
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
            stillingsinfoService.hentForStilling(id)

        val stilling = dto.stilling.copyMedBeregnetTitle(
            stillingskategori = eksisterendeStillingsinfo?.stillingskategori
        )

        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling, queryString)

        log.info("Stilling som sendes til pam-ad-api for oppdatering: $stilling")
        log.info("Stilling som mottas fra pam-ad-api for oppdatering: $oppdatertStilling")

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

    private fun opprettDirektemeldtStilling(opprettStilling: OpprettStillingDto, uuid: UUID) : DirektemeldtStilling {
        val stillingsInnhold = lagDirektemeldtStillingInnhold(opprettStilling, uuid.toString())
        val direktemeldtStilling = DirektemeldtStilling(
            stillingsId = uuid,
            innhold = stillingsInnhold,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = opprettStilling.createdBy,
            sistEndretAv = "pam-rekrutteringsbistand",
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            status = "INACTIVE",
            annonseId = null
        )
        return direktemeldtStilling
    }

    private fun lagDirektemeldtStillingInnhold(opprettStilling: OpprettStillingDto, uuid: String) : DirektemeldtStillingInnhold {
        val opprettetTidspunkt = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        val opprettetDirektemeldStilling = DirektemeldtStillingInnhold(
            title = "Ny stilling",
            administration = opprettStilling.administration.toDirekteMeldtStillingAdministration(),
            mediaList = emptyList(),
            contactList = emptyList(),
            privacy = opprettStilling.privacy,
            source = opprettStilling.source,
            medium = opprettStilling.source,
            reference = uuid,
            published = opprettetTidspunkt,
            expires = opprettetTidspunkt,
            employer = opprettStilling.employer?.toDirektemeldtStillingArbeidsgiver(),
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

        return opprettetDirektemeldStilling
    }

    fun lagreDirektemeldtStilling(stillingsId: String) {
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, true)
        log.info("Stilling som ville blitt lagret ved oppdatering fra pam-ad: $stilling")

        val direktemeldtStillingBlob = stilling.toDirektemeldtStillingInnhold()

        val direktemeldtStilling = DirektemeldtStilling(
            stillingsId = UUID.fromString(stillingsId),
            innhold = direktemeldtStillingBlob,
            opprettet = stilling.created.atZone(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = stilling.updated.atZone(ZoneId.of("Europe/Oslo")),
            status = stilling.status,
            annonseId = stilling.id
        )
        log.info("Direktemeldt stilling som blir lagret ved melding på kafka: $direktemeldtStilling")

       // direktemeldtStillingRepository.lagreDirektemeldtStilling(direktemeldtStilling)
    }
}
