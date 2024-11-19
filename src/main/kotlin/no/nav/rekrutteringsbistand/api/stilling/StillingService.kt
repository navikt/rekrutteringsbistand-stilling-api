package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
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

    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr: String): Option<RekrutteringsbistandStilling> {
        val stillinger = arbeidsplassenKlient.hentStillingBasertPåAnnonsenr(annonsenr)
        return stillinger.map { stilling ->
            val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(stilling)

            RekrutteringsbistandStilling(
                stilling = stilling,
                stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null }
            )
        }
    }



    fun opprettNyStilling(opprettDto: OpprettRekrutteringsbistandstillingDto): RekrutteringsbistandStilling {
        return opprettStilling(
            opprettStilling = opprettDto.stilling.toArbeidsplassenDto(title = "Ny stilling"),
            stillingskategori = opprettDto.kategori,
        )
    }

    private fun opprettStilling(opprettStilling: OpprettStillingDto, stillingskategori: Stillingskategori): RekrutteringsbistandStilling {
        val opprettetStilling = arbeidsplassenKlient.opprettStilling(opprettStilling)
        val stillingsId = Stillingsid(opprettetStilling.uuid)

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = stillingskategori
        )

        // Lagre stillingen i intern_stilling
        val internStilling = opprettInternStilling(opprettetStilling, opprettetStilling.uuid)
        internStillingRepository.lagreInternStilling(internStilling)

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStilling)

        return RekrutteringsbistandStilling(
            stilling = opprettetStilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.orNull()
        ).also {
            kandidatlisteKlient.sendStillingOppdatert(it)
        }
    }

    private fun opprettInternStilling(opprettStilling: Stilling, stillingsId: String) : InternStilling {
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

        val internStilling = opprettInternStilling(dto.stilling, id.asString())
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

        val internStilling = InternStilling(
            UUID.fromString(stillingsId),
            stilling,
            opprettet = ZonedDateTime.now(ZoneId.of("Europe/Oslo")),
            opprettetAv = stilling.createdBy,
            sistEndretAv = stilling.updatedBy,
            sistEndret = ZonedDateTime.now(ZoneId.of("Europe/Oslo"))
        )
        internStillingRepository.lagreInternStilling(internStilling)
    }
}
