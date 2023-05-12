package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Option
import arrow.core.Some
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.log
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit


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

    private fun lagreNyttNotat(
        nyttNotat: String,
        stillingsId: Stillingsid,
    ) {
        val eksisterendeStillingsinfoId = stillingsinfoService.hentForStilling(stillingsId).map { it.stillingsinfoid }

        if (eksisterendeStillingsinfoId is Some) {
            val nyOppdaterNotat = OppdaterNotat(eksisterendeStillingsinfoId.value, nyttNotat)
            stillingsinfoService.oppdaterNotat(stillingsId, nyOppdaterNotat)
        } else {
            val nyStillingsinfo = Stillingsinfo(
                stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
                stillingsid = stillingsId,
                notat = nyttNotat,
                eier = null,
                stillingskategori = null
            )
            stillingsinfoService.lagre(nyStillingsinfo)
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
        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(dto.stilling, queryString)

        val id = Stillingsid(oppdatertStilling.uuid)

        if (dto.notat != null) {
            lagreNyttNotat(dto.notat, id)
        }

        val eksisterendeStillingsinfo: Stillingsinfo? =
            stillingsinfoService.hentForStilling(id).orNull()

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = oppdatertStilling,
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString(),
            notat = eksisterendeStillingsinfo?.notat
        ).also {
            if (oppdatertStilling.source.equals("DIR", ignoreCase = false)) {
                kandidatlisteKlient.sendStillingOppdatert(RekrutteringsbistandStilling(
                    stilling = oppdatertStilling,
                    stillingsinfo = eksisterendeStillingsinfo?.asStillingsinfoDto()
                ))
            }
        }
    }

    fun slettRekrutteringsbistandStilling(stillingsId: String): Stilling {
        kandidatlisteKlient.varsleOmSlettetStilling(Stillingsid(stillingsId))
        return arbeidsplassenKlient.slettStilling(stillingsId)
    }

    fun hentMineStillinger(queryString: String?): Page<RekrutteringsbistandStilling> {
        val startHenteFraPam = System.nanoTime()
        val stillingerPage = arbeidsplassenKlient.hentMineStillinger(queryString)
        log.info("Brukte ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startHenteFraPam)} millisekunder på å hente stillinger fra PAM")

        val startMappeTilStillingsIder = System.nanoTime()
        val stillingsIder = stillingerPage.content.map { Stillingsid(it.uuid) }
        log.info("Brukte ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startMappeTilStillingsIder)} millisekunder på å mappe til stillingsIder)")

        val startHenteStillingsinfoOgLageMap = System.nanoTime()
        val stillingsinfoer: Map<String, Stillingsinfo> = stillingsinfoService
            .hentForStillinger(stillingsIder)
            .associateBy { it.stillingsid.asString() }
        log.info("Brukte ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startHenteStillingsinfoOgLageMap)} millisekunder på å hente stillingsinfo og lage map med stillingId som key)")

        val startSlåSammenStillingOgStillingsinfo = System.nanoTime()
        val rekrutteringsbistandStillinger = stillingerPage.content.map {
            RekrutteringsbistandStilling(
                stillingsinfo = stillingsinfoer[it.uuid]?.asStillingsinfoDto(),
                stilling = it
            )
        }
        val tidBruktMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startSlåSammenStillingOgStillingsinfo)
        log.info("Brukte $tidBruktMillis millisekunder på å slå sammen stillingsinfo med stilling for ${stillingsIder.size} stillinger")

        return Page(
            totalPages = stillingerPage.totalPages,
            totalElements = stillingerPage.totalElements,
            content = rekrutteringsbistandStillinger
        )
    }
}
