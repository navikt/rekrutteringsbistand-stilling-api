package no.nav.rekrutteringsbistand.api

import no.nav.security.spring.oidc.api.EnableOIDCTokenValidation
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
//@EnableOIDCTokenValidation(ignore = ["springfox.documentation.swagger.web.ApiResourceController", "org.springframework"])
class RekrutteringsbistandApplication
    fun main(args: Array<String>) {
        runApplication<RekrutteringsbistandApplication>(*args)
    }
