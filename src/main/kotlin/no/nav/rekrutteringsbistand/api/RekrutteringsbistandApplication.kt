package no.nav.rekrutteringsbistand.api

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableJwtTokenValidation(ignore = [
    "org.springframework",
    "springfox.documentation.swagger.web.ApiResourceController"
])
class RekrutteringsbistandApplication {
    fun main(args: Array<String>) {
        runApplication<RekrutteringsbistandApplication>(*args)
    }
}
