package no.nav.rekrutteringsbistand.api.requester

import no.nav.rekrutteringsbistand.api.konfigurasjon.Configuration
import no.nav.rekrutteringsbistand.api.konfigurasjon.ExternalConfiguration
import no.nav.rekrutteringsbistand.api.requester.support.RestProxy
import no.nav.security.oidc.api.Protected
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@Protected
class StillingController(
        val restProxy: RestProxy,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val externalConfiguration: ExternalConfiguration) {


    @RequestMapping("/rekrutteringsbistand/api/v1/**")
    fun stilling(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?): ResponseEntity<String> {
        return restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body
                ?: "", externalConfiguration.stillingApi.url)
    }

    @RequestMapping("/search-api/**")
    private fun sok(method: HttpMethod, request: HttpServletRequest, @RequestBody body: String = ""): ResponseEntity<String> =
            restProxy.proxyJsonRequest(method, request, Configuration.ROOT_URL, body, externalConfiguration.stillingApi.url)

}
