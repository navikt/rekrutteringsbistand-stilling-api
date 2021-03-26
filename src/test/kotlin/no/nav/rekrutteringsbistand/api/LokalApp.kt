package no.nav.rekrutteringsbistand.api

import org.springframework.boot.builder.SpringApplicationBuilder

fun main(args: Array<String>) {
    SpringApplicationBuilder(RekrutteringsbistandApplication::class.java)
        .profiles("default", "sokMock", "kandidatlisteMock", "stillingMock")
        .run(*args)
}
