package no.nav.rekrutteringsbistand.api.sok

import no.nav.security.oidc.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/search-api/**")
@ProtectedWithClaims(issuer = "loginservice-oidc")
class SokController {

    @RequestMapping
    fun sok(): ResponseEntity<HttpStatus> {
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
    }
}
