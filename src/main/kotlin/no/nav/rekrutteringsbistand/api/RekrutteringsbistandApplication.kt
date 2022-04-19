package no.nav.rekrutteringsbistand.api

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EnableJwtTokenValidation(ignore = [
    "org.springframework"
])
class RekrutteringsbistandApplication

fun main(args: Array<String>) {
    runApplication<RekrutteringsbistandApplication>(*args)
}
