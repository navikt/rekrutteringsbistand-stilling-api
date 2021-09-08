package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.RekrutteringsbistandStilling
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.proxy.RestProxy
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.*
import javax.servlet.http.HttpServletRequest

@Service
class StillingService(
    val restTemplate: RestTemplate,
    val externalConfiguration: ExternalConfiguration,
    val stillingsinfoService: StillingsinfoService,
    val tokenUtils: TokenUtils,
    val kandidatlisteKlient: KandidatlisteKlient,
    val restProxy: RestProxy
) {

    fun hentRekrutteringsbistandStilling(uuid: String): RekrutteringsbistandStilling {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$uuid"
        val opprinneligStilling: Stilling = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headersUtenToken()),
            Stilling::class.java
        ).body!!

        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(opprinneligStilling)

        return RekrutteringsbistandStilling(
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null },
            stilling = opprinneligStilling
        )
    }

    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr: String): RekrutteringsbistandStilling {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads"
        val queryParams = "id=${annonsenr}"
        val stilling = hent(url, queryParams).content.first()

        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(stilling)

        return RekrutteringsbistandStilling(
            stilling = stilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null }
        )
    }

    fun opprettStilling(stilling: Stilling, queryString: String?): RekrutteringsbistandStilling {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads"
        val opprinneligStilling = restTemplate.exchange(
            url + if (queryString != null) "?$queryString" else "",
            HttpMethod.POST,
            HttpEntity(stilling, headers()),
            Stilling::class.java
        )
            .body
            ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra opprett stilling")

        val id = opprinneligStilling.uuid?.let { Stillingsid(it) }
            ?: throw IllegalArgumentException("Mangler stilling uuid")

        kandidatlisteKlient.oppdaterKandidatliste(id)
        val stillingsinfo: Option<Stillingsinfo> = stillingsinfoService.hentStillingsinfo(opprinneligStilling)

        return RekrutteringsbistandStilling(
            stilling = opprinneligStilling,
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

    fun slettStilling(uuid: String, request: HttpServletRequest): ResponseEntity<String> {
        val respons = restProxy.proxyJsonRequest(
            HttpMethod.DELETE,
            request,
            "/rekrutteringsbistand",
            null,
            externalConfiguration.stillingApi.url
        )
        kandidatlisteKlient.oppdaterKandidatliste(Stillingsid(uuid))
        return respons
    }

    fun hentMineStillinger(queryString: String?): Page<RekrutteringsbistandStilling> {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/rekrutteringsbistand/minestillinger"
        val stillingerPage: Page<Stilling> = hent(url, queryString)

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

    private fun hent(url: String, queryString: String?): Page<Stilling> {

        val withQueryParams: String = UriComponentsBuilder.fromHttpUrl(url).query(queryString).build().toString()
        val stillingPage = restTemplate.exchange(
            withQueryParams,
            HttpMethod.GET,
            HttpEntity(null, headers()),
            object : ParameterizedTypeReference<Page<Stilling>>() {}
        ).body
            ?: throw RestResponseEntityExceptionHandler.NoContentException("Ingen body på henting av stilling, url: $url")

        return stillingPage
    }

    private fun headers() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.AUTHORIZATION to "Bearer ${tokenUtils.hentOidcToken()}}"
        ).toMultiValueMap()

    private fun headersUtenToken() =
        mapOf(
            HttpHeaders.CONTENT_TYPE to MediaType.APPLICATION_JSON_VALUE,
            HttpHeaders.ACCEPT to MediaType.APPLICATION_JSON_VALUE
        ).toMultiValueMap()


}
