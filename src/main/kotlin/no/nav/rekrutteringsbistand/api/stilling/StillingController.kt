package no.nav.rekrutteringsbistand.api.stilling

import no.nav.security.oidc.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/rekrutteringsbistand/api/v1/**")
@ProtectedWithClaims(issuer = "loginservice-oidc")
class StillingController {

    @RequestMapping
    fun stilling() : ResponseEntity<HttpStatus> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }

}
