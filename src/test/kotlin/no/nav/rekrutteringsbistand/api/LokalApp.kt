package no.nav.rekrutteringsbistand.api

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.boot.runApplication
import java.time.ZonedDateTime

fun main(args: Array<String>) {
    runApplication<RekrutteringsbistandApplication>(*args) {
        setAdditionalProfiles("default", "sokMock", "kandidatlisteMock", "stillingMock")
    }
}

fun JsonNode.asZonedDateTime(): ZonedDateTime = ZonedDateTime.parse(asText())