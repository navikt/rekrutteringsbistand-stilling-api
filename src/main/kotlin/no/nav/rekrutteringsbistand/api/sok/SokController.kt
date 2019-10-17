package no.nav.rekrutteringsbistand.api.sok

import no.nav.rekrutteringsbistand.api.BaseRestProxyController
import no.nav.rekrutteringsbistand.api.ExternalConfiguration
import no.nav.security.oidc.api.ProtectedWithClaims
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/search-api/**")
@ProtectedWithClaims(issuer = "loginservice-oidc")
class SokController(restTemplateBuilder: RestTemplateBuilder, externalConfiguration: ExternalConfiguration) : BaseRestProxyController(restTemplateBuilder.build(), externalConfiguration.stillingApi.url ) {

    @RequestMapping
    private fun sok(method: HttpMethod, request: HttpServletRequest, @RequestBody body: String = "") : ResponseEntity<String> {
        //return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
        return proxyJsonRequest(method,request,ROOT_URL, body)
    }
}
