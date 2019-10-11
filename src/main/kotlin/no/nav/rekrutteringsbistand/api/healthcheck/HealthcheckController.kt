package no.nav.rekrutteringsbistand.api.healthcheck

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthcheckController {

    @GetMapping("/internal/healthcheck")
    fun healthcheck(): String {
        return "ok"
    }
}
