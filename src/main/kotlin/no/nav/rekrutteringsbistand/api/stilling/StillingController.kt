package no.nav.rekrutteringsbistand.api.stilling

import no.nav.rekrutteringsbistand.api.BaseRestProxyController
import no.nav.rekrutteringsbistand.api.ExternalConfiguration
import no.nav.security.oidc.api.ProtectedWithClaims
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/**")
@ProtectedWithClaims(issuer = "loginservice-oidc")
class StillingController(restTemplateBuilder: RestTemplateBuilder, externalConfiguration: ExternalConfiguration) : BaseRestProxyController(restTemplateBuilder.build(), externalConfiguration.stillingApi.url ) {

    @RequestMapping
    fun stilling(method: HttpMethod, request: HttpServletRequest, @RequestBody(required = false) body: String?) : ResponseEntity<String> {
        return proxyJsonRequest(method,request,"/rekrutteringsbistand-api/rekrutteringsbistand", body?:"")
    }

}
