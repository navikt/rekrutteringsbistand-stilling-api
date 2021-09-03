package no.nav.rekrutteringsbistand.api.stilling

import arrow.core.Either
import arrow.core.getOrElse
import no.nav.rekrutteringsbistand.api.HentRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.OppdaterRekrutteringsbistandStillingDto
import no.nav.rekrutteringsbistand.api.autorisasjon.TokenUtils
import no.nav.rekrutteringsbistand.api.kandidatliste.KandidatlisteKlient
import no.nav.rekrutteringsbistand.api.option.Option
import no.nav.rekrutteringsbistand.api.option.Some
import no.nav.rekrutteringsbistand.api.option.get
import no.nav.rekrutteringsbistand.api.stillingsinfo.*
import no.nav.rekrutteringsbistand.api.support.config.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.support.rest.RestProxy
import no.nav.rekrutteringsbistand.api.support.rest.RestResponseEntityExceptionHandler
import no.nav.rekrutteringsbistand.api.support.toMultiValueMap
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
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

    @Deprecated("Bruk hentRekrutteringsbistandStilling")
    fun hentStilling(uuid: String): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$uuid"
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headersUtenToken()),
            StillingMedStillingsinfo::class.java
        ).body!!
        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asEierDto()) }
            .getOrElse { opprinneligStilling }
    }

    fun hentRekrutteringsbistandStilling(uuid: String): HentRekrutteringsbistandStillingDto {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads/$uuid"
        val opprinneligStilling: Stilling = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(null, headersUtenToken()),
            Stilling::class.java
        ).body!!

        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)

        return HentRekrutteringsbistandStillingDto(
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null },
            stilling = opprinneligStilling
        )
    }

    fun hentRekrutteringsbistandStillingBasertPåAnnonsenr(annonsenr: String): HentRekrutteringsbistandStillingDto {
        val url = "${externalConfiguration.stillingApi.url}/b2b/api/v1/ads"
        val queryParams = "id=${annonsenr}"
        val stilling = hent(url, queryParams).content.first()

        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(stilling)

        return HentRekrutteringsbistandStillingDto(
            stilling = stilling,
            stillingsinfo = stillingsinfo.map { it.asStillingsinfoDto() }.getOrElse { null }
        )
    }

    fun opprettStilling(stilling: Stilling, queryString: String?): StillingMedStillingsinfo {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads"
        val opprinneligStilling: StillingMedStillingsinfo = restTemplate.exchange(
            url + if (queryString != null) "?$queryString" else "",
            HttpMethod.POST,
            HttpEntity(stilling, headers()),
            StillingMedStillingsinfo::class.java
        )
            .body
            ?: throw RestResponseEntityExceptionHandler.NoContentException("Tom body fra opprett stilling")

        val id = opprinneligStilling.uuid?.let { Stillingsid(it) }
            ?: throw IllegalArgumentException("Mangler stilling uuid")
        kandidatlisteKlient.oppdaterKandidatliste(id)
        val stillingsinfo: Option<Stillingsinfo> = hentStillingsinfo(opprinneligStilling)
        return stillingsinfo.map { opprinneligStilling.copy(rekruttering = it.asEierDto()) }
            .getOrElse { opprinneligStilling }
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

    fun hentMineStillinger(queryString: String?): Page<HentRekrutteringsbistandStillingDto> {
        val url = "${externalConfiguration.stillingApi.url}/api/v1/ads/rekrutteringsbistand/minestillinger"
        val stillingerPage: Page<Stilling> = hent(url, queryString)

        val stillingsIder = stillingerPage.content.map { Stillingsid(it.uuid!!) }

        val stillingsinfoer: Map<String, Stillingsinfo> = stillingsinfoService
            .hentForStillinger(stillingsIder)
            .associateBy { it.stillingsid.asString() }

        val rekrutteringsbistandStillinger = stillingerPage.content.map {
            HentRekrutteringsbistandStillingDto(
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

    fun hentStillinger(url: String, queryString: String?): Page<StillingMedStillingsinfo> {
        val opprinneligeStillingerPage: Page<StillingMedStillingsinfo> = hentGammel(url, queryString, headers())
            ?: throw RestResponseEntityExceptionHandler.NoContentException("Fant ikke stillinger")

        val opprinneligeStillinger: List<StillingMedStillingsinfo> = opprinneligeStillingerPage.content

        val stillingsinfoer: List<Either<Unit, Stillingsinfo>> = opprinneligeStillinger.map(::hentStillingsinfo)

        val newContent = stillingsinfoer.zip(opprinneligeStillinger, ::leggPåStillingsinfo)

        return opprinneligeStillingerPage.copy(content = newContent)
    }

    private fun leggPåStillingsinfo(
        info: Option<Stillingsinfo>,
        opprinnelig: StillingMedStillingsinfo
    ): StillingMedStillingsinfo {
        return info.map { opprinnelig.copy(rekruttering = it.asEierDto()) }.getOrElse { opprinnelig }
    }

    private fun hent(url: String, queryString: String?): Page<Stilling> {

        val withQueryParams: String = UriComponentsBuilder.fromHttpUrl(url).query(queryString).build().toString()
        val stillingPage = restTemplate.exchange(
            withQueryParams,
            HttpMethod.GET,
            HttpEntity(null, headers()),
            object : ParameterizedTypeReference<Page<Stilling>>() {}
        ).body

        if (stillingPage == null || stillingPage.content.isEmpty()) {
            throw RestResponseEntityExceptionHandler.NoContentException("Ingen body på henting av stilling, url: $url")
        }

        return stillingPage
    }

    // TODO: Slett denne siden den returnerer Page<StillingMedStillingsinfo>, noe som ikke stemmer.
    //  Vi får faktisk en Page<Stilling> fra API-kallet.
    //  Kan ikke lett oppdatere typen, siden resten av koden avhenger av at det er feil typesetting.
    //  Når @Deprecated endepunkt er fjerna kan vi bare slette denne koden.
    private fun hentGammel(
        url: String,
        queryString: String?,
        headers: MultiValueMap<String, String>
    ): Page<StillingMedStillingsinfo>? {
        val withQueryParams: String = UriComponentsBuilder.fromHttpUrl(url).query(queryString).build().toString()
        return restTemplate.exchange(
            withQueryParams,
            HttpMethod.GET,
            HttpEntity(null, headers),
            object : ParameterizedTypeReference<Page<StillingMedStillingsinfo>>() {}
        ).body
    }

    private fun hentStillingsinfo(stillingMedStillingsinfo: StillingMedStillingsinfo): Option<Stillingsinfo> =
        stillingsinfoService.hentForStilling(Stillingsid(stillingMedStillingsinfo.uuid!!))

    private fun hentStillingsinfo(stilling: Stilling): Option<Stillingsinfo> =
        stillingsinfoService.hentForStilling(Stillingsid(stilling.uuid!!))

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
