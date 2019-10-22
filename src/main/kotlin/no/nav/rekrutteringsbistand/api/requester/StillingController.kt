package no.nav.rekrutteringsbistand.api.requester

import no.nav.rekrutteringsbistand.api.konfigurasjon.ExternalConfiguration
import no.nav.security.oidc.api.Protected
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@Protected
class StillingController(
        restTemplateBuilder: RestTemplateBuilder,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") externalConfiguration: ExternalConfiguration
) : BaseRestProxyController(restTemplateBuilder.build(), externalConfiguration.stillingApi.url) {

    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    fun stilling(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> =
            proxyJsonRequest(method, request, "$ROOT_URL/rekrutteringsbistand", body ?: "")

    @RequestMapping("/search-api/**")
    private fun sok(method: HttpMethod, request: HttpServletRequest, @RequestBody body: String = ""): ResponseEntity<String> =
            proxyJsonRequest(method, request, ROOT_URL, body)

}
