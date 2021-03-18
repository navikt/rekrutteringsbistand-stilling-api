package no.nav.rekrutteringsbistand.api.support.featuretoggle

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "isso")
class FeatureToggleController(private val featureToggleService: FeatureToggleService) {

    @GetMapping("/features/{feature}")
    fun isEnabled(@PathVariable feature: String): ResponseEntity<Boolean> {
        return ResponseEntity.ok(featureToggleService.isEnabled(feature))
    }
}
