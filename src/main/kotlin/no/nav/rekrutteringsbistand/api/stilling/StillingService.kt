package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*


@Service
class StillingService(
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient
) {
    fun hentRekrutteringsbistandStilling(
        stillingsId: String,
        somSystembruker: Boolean = false
    ): RekrutteringsbistandStilling {
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId, somSystembruker)
        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(stilling)

        return RekrutteringsbistandStilling(
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null },
            stilling = stilling
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

    fun opprettStilling(opprettRekrutteringsbistandstillingDto: OpprettRekrutteringsbistandstillingDto): RekrutteringsbistandStilling {
        // TODO styrk: Ny (interne) stilling skal ha tittel "Ny stilling" hos arbeidsplassen. Den har på dette tidspunktet ikke styrk (antar vi)
        val opprettetStilling = arbeidsplassenKlient.opprettStilling(opprettRekrutteringsbistandstillingDto.stilling)
        val stillingsId = Stillingsid(opprettetStilling.uuid)

        stillingsinfoService.opprettStillingsinfo(
            stillingsId = stillingsId,
            stillingskategori = opprettRekrutteringsbistandstillingDto.kategori
        )

        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStilling)

        return RekrutteringsbistandStilling(
            stilling = opprettetStilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.orNull()
        ).also {
            kandidatlisteKlient.sendStillingOppdatert(it)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val eksisterendeRekrutteringsbistandStilling = hentRekrutteringsbistandStilling(stillingsId)
        val eksisterendeStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val kopi = eksisterendeStilling.toKopiertStilling(tokenUtils)

        return opprettStilling(
            OpprettRekrutteringsbistandstillingDto(
                kopi,
                kategoriMedDefault(eksisterendeRekrutteringsbistandStilling.stillingsinfo)
            )
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

        // TODO styrk: tittel skal være styrk mot arbeidsplassen. for interne stillinger

        val stilling = dto.stilling

        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(stilling, queryString)



        val id = Stillingsid(oppdatertStilling.uuid)

        val eksisterendeStillingsinfo: Stillingsinfo? =
            stillingsinfoService.hentForStilling(id).orNull()

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = oppdatertStilling,
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString()
        ).also {
            if (oppdatertStilling.source.equals("DIR", ignoreCase = false)) {
                kandidatlisteKlient.sendStillingOppdatert(RekrutteringsbistandStilling(
                    stilling = oppdatertStilling,
                    stillingsinfo = eksisterendeStillingsinfo?.asStillingsinfoDto()
                ))
            }
        }
    }

    private fun loggEventuellOvertagelse(dto: OppdaterRekrutteringsbistandStillingDto) {
        val gammelStilling = arbeidsplassenKlient.hentStilling(dto.stilling.uuid)
        val gammelEier = gammelStilling.administration?.navIdent
        val nyEier = dto.stilling.administration?.navIdent
        if (!nyEier.equals(gammelEier)) {
            secureLog.info("Overtar intern stilling og kandidatliste ny-ident $nyEier gammel-ident $gammelEier stillingsid ${dto.stilling.uuid}")
        }
    }

    fun slettRekrutteringsbistandStilling(stillingsId: String): Stilling {
        kandidatlisteKlient.varsleOmSlettetStilling(Stillingsid(stillingsId))
        return arbeidsplassenKlient.slettStilling(stillingsId)
    }
}
