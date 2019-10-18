package no.nav.rekrutteringsbistand.api

import no.nav.security.oidc.test.support.spring.TokenGeneratorConfiguration
import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

fun main(args: Array<String>) {
    runApplication<RekrutteringsbistandApplication>(*args)
}

@Import(value = [TokenGeneratorConfiguration::class])
@SpringBootApplication
@EnableOIDCTokenValidation(ignore = ["org.springframework"])
class RekrutteringsbistandApplication
