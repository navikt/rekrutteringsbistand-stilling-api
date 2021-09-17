package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.arbeidsplassen.OpprettRekrutteringsbistandstillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import org.springframework.stereotype.Service
import java.util.*


@Service
class StillingService(
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val arbeidsplassenKlient: ArbeidsplassenKlient
) {
    fun hentRekrutteringsbistandStilling(stillingsId: String): RekrutteringsbistandStilling {
        val stilling = arbeidsplassenKlient.hentStilling(stillingsId)
        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(stilling)

        return RekrutteringsbistandStilling(
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null },
            stilling = stilling
        )
    }

    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr: String): RekrutteringsbistandStilling {
        val stilling = arbeidsplassenKlient.hentStillingBasertPåAnnonsenr(annonsenr)
        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(stilling)

        return RekrutteringsbistandStilling(
            stilling = stilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null }
        )
    }

    fun opprettStilling(opprettRekrutteringsbistandstillingDto: OpprettRekrutteringsbistandstillingDto): RekrutteringsbistandStilling {
        val opprettetStilling = arbeidsplassenKlient.opprettStilling(opprettRekrutteringsbistandstillingDto.stilling)
        stillingsinfoService.opprettStillingInfo(
            Stillingsid(opprettetStilling.uuid),
            opprettRekrutteringsbistandstillingDto.oppdragkategori
        )
        val id = Stillingsid(opprettetStilling.uuid)

        kandidatlisteKlient.varsleOmOppdatertStilling(id)
        val stillingsinfo = stillingsinfoService.hentStillingsinfo(opprettetStilling)

        return RekrutteringsbistandStilling(
            stilling = opprettetStilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.orNull()
        )
    }

    private fun lagreNyttNotat(
        nyttNotat: String,
        stillingsId: Stillingsid,
    ) {
        val eksisterendeStillingsinfoId = stillingsinfoService.hentForStilling(stillingsId).map { it.stillingsinfoid }

        if (eksisterendeStillingsinfoId is Some) {
            val nyOppdaterNotat = OppdaterNotat(eksisterendeStillingsinfoId.get(), nyttNotat)
            stillingsinfoService.oppdaterNotat(stillingsId, nyOppdaterNotat)
        } else { //
            val nyStillingsinfo = Stillingsinfo(
                stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
                stillingsid = stillingsId,
                notat = nyttNotat,
                eier = null,
                oppdragKategori = null
                // TODO: Vi antar at vi alltid har stillingsinfo når vi legger til nye stillinger.
                // Kategori skal fortsatt være null på gamle stillinger. Vi vil ikke legge til kategori på gamle stillinger
                // når vi legger til notat. Det er frontend som vil få ansvaret for å tolke manglende kategori som at dette er en stilling.

            )
            stillingsinfoService.lagre(nyStillingsinfo)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val eksisterendeRekrutteringsbistandStilling = hentRekrutteringsbistandStilling(stillingsId)
        val eksisterendeStilling = eksisterendeRekrutteringsbistandStilling.stilling
        val kopi = eksisterendeStilling.toKopiertStilling(tokenUtils)

        return opprettStilling(OpprettRekrutteringsbistandstillingDto(kopi, kategoriMedDefault(eksisterendeRekrutteringsbistandStilling.stillingsinfo)))
    }

    fun kategoriMedDefault(stillingsInfo: StillingsinfoDto?) =
        if (stillingsInfo?.oppdragKategori == null) OppdragKategori.Stilling else stillingsInfo.oppdragKategori

    fun oppdaterRekrutteringsbistandStilling(
        dto: OppdaterRekrutteringsbistandStillingDto,
        queryString: String?
    ): OppdaterRekrutteringsbistandStillingDto {
        val oppdatertStilling = arbeidsplassenKlient.oppdaterStilling(dto.stilling, queryString)

        val id = Stillingsid(oppdatertStilling.uuid)

        if (oppdatertStilling.source.equals("DIR", false)) {
            kandidatlisteKlient.varsleOmOppdatertStilling(id)
        }

        if (dto.notat != null) {
            lagreNyttNotat(dto.notat, id)
        }

        val eksisterendeStillingsinfo: Stillingsinfo? =
            stillingsinfoService.hentForStilling(id).orNull()

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = oppdatertStilling,
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString(),
            notat = eksisterendeStillingsinfo?.notat
        )
    }

    fun slettStilling(stillingsId: String): Stilling {
        val slettetStilling = arbeidsplassenKlient.slettStilling(stillingsId)
        kandidatlisteKlient.varsleOmOppdatertStilling(Stillingsid(stillingsId))
        return slettetStilling
    }

    fun hentMineStillinger(queryString: String?): Page<RekrutteringsbistandStilling> {
        val stillingerPage = arbeidsplassenKlient.hentMineStillinger(queryString)

        val stillingsIder = stillingerPage.content.map { Stillingsid(it.uuid) }

        val stillingsinfoer: Map<String, Stillingsinfo> = stillingsinfoService
            .hentForStillinger(stillingsIder)
            .associateBy { it.stillingsid.asString() }

        val rekrutteringsbistandStillinger = stillingerPage.content.map {
            RekrutteringsbistandStilling(
                stillingsinfo = stillingsinfoer[it.uuid]?.asStillingsinfoDto(),
                stilling = it
            )
        }

        return Page(
            totalPages = stillingerPage.totalPages,
            totalElements = stillingerPage.totalElements,
            content = rekrutteringsbistandStillinger
        )
    }
}
