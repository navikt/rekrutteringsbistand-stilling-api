package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.arbeidsplassen.ArbeidsplassenKlient
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*


@Service
class StillingService(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
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

    fun opprettStilling(stilling: OpprettStillingDto): RekrutteringsbistandStilling {
        val opprettetStilling = arbeidsplassenKlient.opprettStilling(stilling)
        val id = opprettetStilling.uuid?.let { Stillingsid(it) }
            ?: throw IllegalArgumentException("Mangler stilling uuid")

        kandidatlisteKlient.oppdaterKandidatliste(id)
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
        } else {
            val nyStillingsinfo = Stillingsinfo(
                stillingsinfoid = Stillingsinfoid(UUID.randomUUID()),
                stillingsid = stillingsId,
                notat = nyttNotat,
                eier = null
            )
            stillingsinfoService.lagre(nyStillingsinfo)
        }
    }

    fun kopierStilling(stillingsId: String): RekrutteringsbistandStilling {
        val eksisterendeStilling = hentRekrutteringsbistandStilling(stillingsId).stilling

        val kopi = OpprettStillingDto(
            title = "Kopi - ${eksisterendeStilling.title}",
            createdBy = "pam-rekrutteringsbistand",
            updatedBy = "pam-rekrutteringsbistand",
            source = "DIR",
            privacy = "INTERNAL_NOT_SHOWN",
            administration = OpprettStillingAdministrationDto(
                status = "PENDING",
                reportee = tokenUtils.hentInnloggetVeileder().displayName,
                navIdent = tokenUtils.hentInnloggetVeileder().navIdent,
            ),

            mediaList = eksisterendeStilling.mediaList,
            contactList = eksisterendeStilling.contactList,
            medium = eksisterendeStilling.medium,
            employer = eksisterendeStilling.employer,
            location = eksisterendeStilling.location,
            locationList = eksisterendeStilling.locationList,
            categoryList = eksisterendeStilling.categoryList,
            properties = eksisterendeStilling.properties,
            businessName = eksisterendeStilling.businessName,
            firstPublished = eksisterendeStilling.firstPublished,
            deactivatedByExpiry = eksisterendeStilling.deactivatedByExpiry,
            activationOnPublishingDate = eksisterendeStilling.activationOnPublishingDate,
        )

        TODO()
    }


    fun oppdaterRekrutteringsbistandStilling(
        dto: OppdaterRekrutteringsbistandStillingDto,
        queryString: String?
    ): OppdaterRekrutteringsbistandStillingDto {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/${dto.stilling.uuid}"
        val returnertStilling: Stilling = restTemplate.exchange(
            url + if (queryString != null) "?$queryString" else "",
            HttpMethod.PUT,
            HttpEntity(dto.stilling, headers()),
            Stilling::class.java
        )
            .body
            ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra oppdater stilling")

        val id = returnertStilling.uuid?.let { Stillingsid(it) }
            ?: throw IllegalArgumentException("Mangler stilling uuid")

        if ("DIR".equals(returnertStilling.source, false)) {
            kandidatlisteKlient.oppdaterKandidatliste(id)
        }

        if (dto.notat != null) {
            lagreNyttNotat(dto.notat, id)
        }

        val eksisterendeStillingsinfo: Stillingsinfo? =
            stillingsinfoService.hentForStilling(id).orNull()

        return OppdaterRekrutteringsbistandStillingDto(
            stilling = returnertStilling,
            stillingsinfoid = eksisterendeStillingsinfo?.stillingsinfoid?.asString(),
            notat = eksisterendeStillingsinfo?.notat
        )

    }

    fun slettStilling(stillingsId: String): Stilling {
        val slettetStilling = arbeidsplassenKlient.slettStilling(stillingsId)
        kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(stillingsId))
        return slettetStilling
    }

    fun hentMineStillinger(queryString: String?): Page<RekrutteringsbistandStilling> {
        val stillingerPage = arbeidsplassenKlient.hentMineStillinger(queryString)

        val stillingsIder = stillingerPage.content.map { Stillingsid(it.uuid!!) }

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

    private fun headers() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
        ).toMultiValueMap()
}
